package com.ai.base.application.service.impl;

import com.ai.base.application.auth.*;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.common.TraceContextSupport;
import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.application.utils.CurrentUserUtil;
import com.ai.base.infrastructure.config.NacosConfig;
import com.ai.base.infrastructure.enums.NacosDataIdEnum;
import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import com.ai.base.infrastructure.persistence.entity.LoginAuditEntity;
import com.ai.base.infrastructure.persistence.mapper.LoginAuditMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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
    private static final String DEVICE_LIMIT_EXCEEDED = "DEVICE_LIMIT_EXCEEDED";
    private static final RedisScript<String> REPLACE_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>("local oldSessionId = redis.call('HGET', KEYS[1], ARGV[1])\nif not oldSessionId and redis.call('HLEN', KEYS[1]) >= tonumber(ARGV[2]) then return 'DEVICE_LIMIT_EXCEEDED' end\nredis.call('HSET', KEYS[1], ARGV[1], ARGV[3])\nredis.call('PEXPIRE', KEYS[1], ARGV[4])\nreturn oldSessionId or ''", String.class);

    private static final RedisScript<Long> ROLLBACK_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>("if redis.call('HGET', KEYS[1], ARGV[1]) ~= ARGV[2] then return 0 end\nif ARGV[3] == '' then return redis.call('HDEL', KEYS[1], ARGV[1]) end\nreturn redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])", Long.class);
    private static final RedisScript<Long> SWITCH_TENANT_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            local ttl = redis.call('PTTL', KEYS[1])
            if not value or ttl <= 0 then return 0 end
            local session = cjson.decode(value)
            if session.userId ~= ARGV[1] or session.sessionId ~= ARGV[2] then return 0 end
            if session.tenantId ~= ARGV[3] then return -1 end
            session.tenantId = ARGV[4]
            redis.call('SET', KEYS[1], cjson.encode(session), 'PX', ttl)
            return 1
            """, Long.class);
    private final Map<String, LoginAuthenticator> loginAuthenticatorMap;
    private final TenantUserExtensionMapper tenantUserExtensionMapper;
    private final LoginAuditMapper loginAuditMapper;
    private final TenantUserService tenantUserService;
    private final UserTenantPreferenceService preferenceService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final NacosConfig nacosConfig;
    private final Executor loginAuditExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginServiceImpl(List<LoginAuthenticator> loginAuthenticators,
                            TenantUserExtensionMapper tenantUserExtensionMapper,
                            LoginAuditMapper loginAuditMapper, StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper, NacosConfig nacosConfig,
                            @Qualifier("loginAuditExecutor") Executor loginAuditExecutor,
                            TenantUserService tenantUserService, UserTenantPreferenceService preferenceService) {
        this.loginAuthenticatorMap = loginAuthenticators.stream()
                .collect(Collectors.toMap(authenticator -> authenticator.supports().getValue(), Function.identity()));
        this.tenantUserExtensionMapper = tenantUserExtensionMapper;
        this.loginAuditMapper = loginAuditMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.nacosConfig = nacosConfig;
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
        String sessionId = IdWorker.getIdStr();
        String sessionToken = newSessionToken();
        String tokenHash = sha256(sessionToken);
        SessionInfo session = generateSession(sessionId, command);
        SessionPolicy sessionPolicy = generateSessionPolicy();
        
        String oldSessionId = replaceDeviceSession(userId, command.getDeviceId(), sessionId, sessionPolicy.getTtl());
        try {
            stringRedisTemplate.opsForValue().set(sessionKey(tokenHash), sessionValue(session, userId, tenantId, sessionPolicy), sessionPolicy.getTtl());
            stringRedisTemplate.opsForValue().set(sessionIdKey(sessionId), tokenHash, sessionPolicy.getTtl());
        } catch (RuntimeException exception) {
            rollbackDeviceSession(userId, command.getDeviceId(), sessionId, oldSessionId);
            throw exception;
        }
        if (!oldSessionId.isEmpty()) {
            deleteSessionValues(oldSessionId);
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
            if (StringUtils.hasText(preference.getDefaultTenantId())) {
                UserTenantRow tenant = tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(userId, preference.getDefaultTenantId());
                if (tenant != null) {
                    return tenant.getTenantId();
                }
            }
            if (StringUtils.hasText(preference.getLastUsedTenantId())) {
                UserTenantRow tenant = tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(userId, preference.getLastUsedTenantId());
                if (tenant != null) {
                    return tenant.getTenantId();
                }
            }
        }
        String tenantId = tenantUserExtensionMapper.selectPersonalTenantId(userId);
        if (tenantId == null) {
            throw new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND);
        }
        return tenantId;
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
        String tokenHash = stringRedisTemplate.opsForValue().get(sessionIdKey(sessionId));
        if (tokenHash == null) {
            throw new BizException(ErrorCodeEnum.SESSION_INVALID);
        }
        LocalDateTime usedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        Long result = stringRedisTemplate.execute(SWITCH_TENANT_SCRIPT, List.of(sessionKey(tokenHash)),
                userId, sessionId, currentTenantId, tenantId);
        if (Long.valueOf(-1).equals(result)) {
            throw new BizException(ErrorCodeEnum.TENANT_CONTEXT_MISMATCH);
        }
        if (!Long.valueOf(1).equals(result)) {
            throw new BizException(ErrorCodeEnum.SESSION_INVALID);
        }
        preferenceService.recordLastUsed(userId, tenantId, usedAt);
        return tenant;
    }

    private String replaceDeviceSession(String userId, String deviceId, String sessionId, Duration sessionTtl) {
        String result = stringRedisTemplate.execute(REPLACE_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, String.valueOf(maxDevices()), sessionId, String.valueOf(sessionTtl.toMillis()));
        if (DEVICE_LIMIT_EXCEEDED.equals(result)) {
            throw new BizException(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED);
        }
        return result == null ? "" : result;
    }

    private void rollbackDeviceSession(String userId, String deviceId, String sessionId, String oldSessionId) {
        stringRedisTemplate.execute(ROLLBACK_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, sessionId, oldSessionId);
    }

    private int maxDevices() {
        BaseAuthConfig config = nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class);
        Integer maxDevices = config == null || config.getSession() == null ? null : config.getSession().getMaxDevices();
        return maxDevices == null || maxDevices <= 0 ? DEFAULT_MAX_DEVICES : maxDevices;
    }

    private SessionPolicy generateSessionPolicy() {
        BaseAuthConfig config = nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class);
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
        LoginAuditEntity audit = new LoginAuditEntity();
        audit.setUserId(userId);
        audit.setTenantId(tenantId);
        audit.setIdentityType(command.getLoginType());
        audit.setIdentityValueHash(sha256(authenticatedIdentity.getIdentityValue()));
        audit.setLoginResult(1);
        audit.setLoginIp(command.getLoginIp());
        audit.setUserAgent(command.getUserAgent());
        audit.setLoginAt(LocalDateTime.now());
        try {
            loginAuditExecutor.execute(TraceContextSupport.wrap(() -> {
                try {
                    loginAuditMapper.insert(audit);
                } catch (Exception exception) {
                    log.error("[LoginAudit] 写入登录审计失败, userId={}", userId, exception);
                }
            }));
        } catch (RuntimeException exception) {
            log.error("[LoginAudit] 审计任务提交失败, userId={}", userId, exception);
        }
    }

    private void deleteSessionValues(String sessionId) {
        String tokenHash = stringRedisTemplate.opsForValue().get(sessionIdKey(sessionId));
        if (tokenHash != null) {
            stringRedisTemplate.delete(sessionKey(tokenHash));
            stringRedisTemplate.delete(sessionIdKey(sessionId));
        }
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
        String tokenHash = stringRedisTemplate.opsForValue().get(sessionIdKey(sessionId));
        if (tokenHash == null) {
            return;
        }
        SessionData session = readSession(tokenHash);
        if (session == null || !userId.equals(session.userId())) {
            return;
        }
        deleteSessionValues(sessionId);
        stringRedisTemplate.opsForHash().delete(userSessionsKey(userId), session.deviceId());
    }

    @Override
    public void logoutAll() {
        String userId = CurrentUserUtil.requireUserId();
        List<Object> sessionIds = stringRedisTemplate.opsForHash().values(userSessionsKey(userId));
        if (sessionIds != null) {
            sessionIds.forEach(sessionId -> logout(userId, sessionId.toString()));
        }
    }

    @Override
    public List<SessionInfo> listSessions() {
        String userId = CurrentUserUtil.requireUserId();
        String currentSessionId = CurrentUserUtil.requireSessionId();
        List<Object> sessionIds = stringRedisTemplate.opsForHash().values(userSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        List<SessionInfo> sessions = new ArrayList<>();
        for (Object sessionId : sessionIds) {
            String value = sessionId.toString();
            String tokenHash = stringRedisTemplate.opsForValue().get(sessionIdKey(value));
            SessionData session = tokenHash == null ? null : readSession(tokenHash);
            if (session == null || !userId.equals(session.userId())) {
                continue;
            }
            SessionInfo info = session.toInfo();
            info.setCurrent(value.equals(currentSessionId));
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

    private String newSessionToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sessionKey(String tokenHash) {
        return "ai-base:auth:session:{" + tokenHash + "}";
    }

    private String sessionIdKey(String sessionId) {
        return "ai-base:auth:session-id:{" + sessionId + "}";
    }

    private String userSessionsKey(String userId) {
        return "ai-base:auth:user-sessions:{" + userId + "}";
    }

    private String sessionValue(SessionInfo info, String userId, String tenantId, SessionPolicy sessionPolicy) {
        try {
            return objectMapper.writeValueAsString(new SessionData(info, userId, tenantId, sessionPolicy));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session", exception);
        }
    }

    private SessionData readSession(String tokenHash) {
        try {
            String value = stringRedisTemplate.opsForValue().get(sessionKey(tokenHash));
            return value == null ? null : objectMapper.readValue(value, SessionData.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private record SessionData(String sessionId, String userId, String tenantId, String deviceId,
                               String loginIp, String userAgent, String loginAt, long ttlMillis, long renewWindowMillis) {
        private SessionData(SessionInfo info, String userId, String tenantId, SessionPolicy sessionPolicy) {
            this(info.getSessionId(), userId, tenantId, info.getDeviceId(), info.getLoginIp(), info.getUserAgent(),
                    info.getLoginAt() == null ? null : info.getLoginAt().toString(), sessionPolicy.getTtl().toMillis(),
                    sessionPolicy.getRenewWindow().toMillis());
        }

        private SessionInfo toInfo() {
            SessionInfo info = new SessionInfo();
            info.setSessionId(sessionId);
            info.setDeviceId(deviceId);
            info.setLoginIp(loginIp);
            info.setUserAgent(userAgent);
            try {
                info.setLoginAt(loginAt == null ? null : LocalDateTime.parse(loginAt));
            } catch (DateTimeParseException ignored) {
                info.setLoginAt(null);
            }
            return info;
        }
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

