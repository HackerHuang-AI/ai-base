package com.ai.base.application.service.impl;

import com.ai.base.application.auth.*;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.common.TraceContextSupport;
import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.model.LoginAuditEntry;
import com.ai.base.application.model.SessionPolicy;
import com.ai.base.application.model.SessionRecord;
import com.ai.base.application.port.AuthPolicyPort;
import com.ai.base.application.port.LoginAuditPort;
import com.ai.base.application.port.SessionStore;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.application.utils.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    private static final Duration DEFAULT_SESSION_TTL = Duration.ofHours(4);
    private static final Duration DEFAULT_SESSION_RENEW_WINDOW = Duration.ofMinutes(10);
    private static final int DEFAULT_MAX_DEVICES = 5;

    private final Map<String, LoginAuthenticator> loginAuthenticatorMap;
    private final SessionStore sessionStore;
    private final AuthPolicyPort authPolicyPort;
    private final LoginAuditPort loginAuditPort;
    private final TenantUserService tenantUserService;
    private final UserTenantPreferenceService preferenceService;
    private final Executor loginAuditExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginServiceImpl(List<LoginAuthenticator> loginAuthenticators,
                            SessionStore sessionStore, AuthPolicyPort authPolicyPort,
                            LoginAuditPort loginAuditPort,
                            @Qualifier("loginAuditExecutor") Executor loginAuditExecutor,
                            TenantUserService tenantUserService, UserTenantPreferenceService preferenceService) {
        this.loginAuthenticatorMap = loginAuthenticators.stream()
                .collect(Collectors.toMap(authenticator -> authenticator.supports().getValue(), Function.identity()));
        this.sessionStore = sessionStore;
        this.authPolicyPort = authPolicyPort;
        this.loginAuditPort = loginAuditPort;
        this.loginAuditExecutor = loginAuditExecutor;
        this.tenantUserService = tenantUserService;
        this.preferenceService = preferenceService;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        // 1. 参数校验
        LoginTypeEnum loginType = LoginTypeEnum.fromValue(command.getLoginType());
        command.setLoginType(loginType.getValue());
        // 2. 身份认证
        AuthenticatedIdentity authenticatedIdentity = findAuthenticator(loginType).authenticate(command);
        String userId = authenticatedIdentity.getUserId();
        // 3. 获取用户最近登录的租户空间
        String tenantId = selectLoginTenantId(userId);
        String sessionId = newSessionId();
        String sessionToken = newSessionToken();
        String tokenHash = sha256(sessionToken);
        SessionInfo session = generateSession(sessionId, command);
        SessionPolicy sessionPolicy = generateSessionPolicy();

        SessionStore.DeviceSlot deviceSlot = replaceDeviceSession(userId, command.getDeviceId(), sessionId,
                sessionPolicy, maxDevices());
        try {
            sessionStore.saveSession(toRecord(session, userId, tenantId, sessionPolicy), tokenHash);
        } catch (RuntimeException exception) {
            sessionStore.rollbackDeviceSlot(userId, command.getDeviceId(), sessionId, deviceSlot.oldSessionId());
            throw exception;
        }
        if (!deviceSlot.oldSessionId().isEmpty()) {
            deleteSessionValues(deviceSlot.oldSessionId());
        }
        preferenceService.recordLastUsed(userId, tenantId, session.getLoginAt().truncatedTo(ChronoUnit.MICROS));
        writeLoginAuditAsync(userId, tenantId, command, authenticatedIdentity);
        // 8. 构造返回结果
        LoginResult result = new LoginResult();
        result.setSessionToken(sessionToken);
        result.setTenantId(tenantId);
        return result;
    }

    private String selectLoginTenantId(String userId) {
        UserTenantPreferenceBO preference = preferenceService.getByUserId(userId);
        if (preference != null) {
            if (StringUtils.hasText(preference.getDefaultTenantId())
                    && userTenantAvailable(userId, preference.getDefaultTenantId())) {
                return preference.getDefaultTenantId();
            }
            if (StringUtils.hasText(preference.getLastUsedTenantId())
                    && userTenantAvailable(userId, preference.getLastUsedTenantId())) {
                return preference.getLastUsedTenantId();
            }
        }
        String tenantId = tenantUserService.findPersonalTenantId(userId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND));
        return tenantId;
    }

    private boolean userTenantAvailable(String userId, String tenantId) {
        return tenantUserService.findAvailable(userId, tenantId).isPresent();
    }

    @Override
    public UserTenantBO switchTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException(ErrorCodeEnum.PARAMETER_REQUIRED);
        }
        String userId = CurrentUserUtil.requireUserId();
        String sessionId = CurrentUserUtil.requireSessionId();
        String currentTenantId = CurrentUserUtil.requireTenantId();
        UserTenantBO tenant = tenantUserService.requireAvailable(userId, tenantId);
        String tokenHash = sessionStore.findTokenHash(sessionId)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.SESSION_INVALID));
        LocalDateTime usedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        SessionStore.TenantSwitchResult result = sessionStore.switchTenant(tokenHash, userId, sessionId,
                currentTenantId, tenantId);
        if (result == SessionStore.TenantSwitchResult.TENANT_MISMATCH) {
            throw new BizException(ErrorCodeEnum.TENANT_CONTEXT_MISMATCH);
        }
        if (result == SessionStore.TenantSwitchResult.SESSION_INVALID) {
            throw new BizException(ErrorCodeEnum.SESSION_INVALID);
        }
        preferenceService.recordLastUsed(userId, tenantId, usedAt);
        return tenant;
    }

    private SessionStore.DeviceSlot replaceDeviceSession(String userId, String deviceId, String sessionId,
                                                         SessionPolicy sessionPolicy, int maxDevices) {
        SessionStore.DeviceSlot deviceSlot = sessionStore.replaceDeviceSlot(userId, deviceId, sessionId,
                sessionPolicy.ttl(), maxDevices);
        if (deviceSlot.deviceLimitExceeded()) {
            throw new BizException(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED);
        }
        return deviceSlot;
    }

    private int maxDevices() {
        BaseAuthConfig config = authPolicyPort.authConfig();
        Integer maxDevices = config == null || config.getSession() == null ? null : config.getSession().getMaxDevices();
        return maxDevices == null || maxDevices <= 0 ? DEFAULT_MAX_DEVICES : maxDevices;
    }

    private SessionPolicy generateSessionPolicy() {
        BaseAuthConfig config = authPolicyPort.authConfig();
        if (config == null || config.getSession() == null) {
            return new SessionPolicy(DEFAULT_SESSION_TTL, DEFAULT_SESSION_RENEW_WINDOW);
        }

        BaseAuthConfig.SessionConfig session = config.getSession();
        Integer ttlMinutes = session.getTtlMinutes();
        Integer renewWindowMinutes = session.getRenewWindowMinutes();
        Duration ttl = ttlMinutes == null || ttlMinutes <= 0 ? DEFAULT_SESSION_TTL : Duration.ofMinutes(ttlMinutes);
        Duration renewWindow = renewWindowMinutes == null || renewWindowMinutes <= 0
                ? DEFAULT_SESSION_RENEW_WINDOW : Duration.ofMinutes(renewWindowMinutes);

        if (renewWindow.compareTo(ttl) >= 0) {
            renewWindow = Duration.ofMillis(ttl.toMillis() / 4);
        }
        return new SessionPolicy(ttl, renewWindow);
    }

    private LoginAuthenticator findAuthenticator(LoginTypeEnum loginType) {
        if (loginType == null) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.LOGIN_TYPE_INVALID);
        }
        LoginAuthenticator authenticator = loginAuthenticatorMap.get(loginType.getValue());
        if (authenticator == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_TYPE_NOT_AVAILABLE);
        }
        return authenticator;
    }

    private void writeLoginAuditAsync(String userId, String tenantId, LoginCommand command,
                                      AuthenticatedIdentity authenticatedIdentity) {
        LoginAuditEntry entry = new LoginAuditEntry(userId, tenantId, command.getLoginType(),
                sha256(authenticatedIdentity.getIdentityValue()), 1, command.getLoginIp(),
                command.getUserAgent(), LocalDateTime.now());
        try {
            loginAuditExecutor.execute(TraceContextSupport.wrap(() -> {
                try {
                    loginAuditPort.record(entry);
                } catch (Exception exception) {
                    log.error("[LoginAudit] 写入登录审计失败, userId={}", userId, exception);
                }
            }));
        } catch (RuntimeException exception) {
            log.error("[LoginAudit] 审计任务提交失败, userId={}", userId, exception);
        }
    }

    private void deleteSessionValues(String sessionId) {
        Optional<String> tokenHash = sessionStore.findTokenHash(sessionId);
        tokenHash.ifPresent(hash -> sessionStore.deleteSession(hash, sessionId));
    }

    @Override
    public void logout() {
        logout(CurrentUserUtil.requireUserId(), CurrentUserUtil.requireSessionId());
    }

    @Override
    public void logout(String sessionId) {
        logout(CurrentUserUtil.requireUserId(), sessionId);
    }

    private void logout(String userId, String sessionId) {
        String tokenHash = sessionStore.findTokenHash(sessionId).orElse(null);
        if (tokenHash == null) {
            return;
        }
        SessionRecord session = sessionStore.findSession(tokenHash).orElse(null);
        if (session == null || !userId.equals(session.userId())) {
            return;
        }
        sessionStore.deleteSession(tokenHash, sessionId);
        sessionStore.removeDevice(userId, session.deviceId());
    }

    @Override
    public void logoutAll() {
        String userId = CurrentUserUtil.requireUserId();
        List<String> sessionIds = sessionStore.listSessionIds(userId);
        sessionIds.forEach(sessionId -> logout(userId, sessionId));
    }

    @Override
    public List<SessionInfo> listSessions() {
        String userId = CurrentUserUtil.requireUserId();
        String currentSessionId = CurrentUserUtil.requireSessionId();
        List<String> sessionIds = sessionStore.listSessionIds(userId);
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        List<SessionInfo> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            String tokenHash = sessionStore.findTokenHash(sessionId).orElse(null);
            SessionRecord session = tokenHash == null ? null : sessionStore.findSession(tokenHash).orElse(null);
            if (session == null || !userId.equals(session.userId())) {
                continue;
            }
            SessionInfo info = toInfo(session);
            info.setCurrent(sessionId.equals(currentSessionId));
            sessions.add(info);
        }
        sessions.sort(Comparator.comparing(SessionInfo::getLoginAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return sessions;
    }


    private SessionInfo generateSession(String sessionId, LoginCommand command) {
        SessionInfo session = new SessionInfo();
        session.setSessionId(sessionId);
        session.setDeviceId(command.getDeviceId());
        session.setLoginIp(command.getLoginIp());
        session.setUserAgent(command.getUserAgent());
        session.setLoginAt(LocalDateTime.now());
        return session;
    }

    private String newSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String newSessionToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private SessionRecord toRecord(SessionInfo info, String userId, String tenantId, SessionPolicy sessionPolicy) {
        return new SessionRecord(info.getSessionId(), userId, tenantId, info.getDeviceId(), info.getLoginIp(),
                info.getUserAgent(), info.getLoginAt(), sessionPolicy.ttl().toMillis(),
                sessionPolicy.renewWindow().toMillis());
    }

    private SessionInfo toInfo(SessionRecord record) {
        SessionInfo info = new SessionInfo();
        info.setSessionId(record.sessionId());
        info.setDeviceId(record.deviceId());
        info.setLoginIp(record.loginIp());
        info.setUserAgent(record.userAgent());
        info.setLoginAt(record.loginAt());
        return info;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                result.append(String.format("%02x", b & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
