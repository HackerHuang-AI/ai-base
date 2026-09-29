package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.LoginResult;
import com.ai.base.application.auth.SessionInfo;
import com.ai.base.application.common.BizException;
import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.utils.CurrentUserUtil;
import com.ai.base.infrastructure.config.NacosConfig;
import com.ai.base.infrastructure.enums.NacosDataIdEnum;
import com.ai.base.infrastructure.persistence.entity.LoginAuditEntity;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.mapper.LoginAuditMapper;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    private static final Duration SESSION_TTL = Duration.ofHours(4);
    private static final int DEFAULT_MAX_DEVICES = 5;
    private static final String DEVICE_LIMIT_EXCEEDED = "DEVICE_LIMIT_EXCEEDED";
    private static final RedisScript<String> REPLACE_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>("local oldSessionId = redis.call('HGET', KEYS[1], ARGV[1])\nif not oldSessionId and redis.call('HLEN', KEYS[1]) >= tonumber(ARGV[2]) then return 'DEVICE_LIMIT_EXCEEDED' end\nredis.call('HSET', KEYS[1], ARGV[1], ARGV[3])\nredis.call('PEXPIRE', KEYS[1], ARGV[4])\nreturn oldSessionId or ''", String.class);

    private static final RedisScript<Long> ROLLBACK_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>("if redis.call('HGET', KEYS[1], ARGV[1]) ~= ARGV[2] then return 0 end\nif ARGV[3] == '' then return redis.call('HDEL', KEYS[1], ARGV[1]) end\nreturn redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])", Long.class);
    private final Map<String, LoginAuthenticator> loginAuthenticatorMap;
    private final UserIdentityMapper userIdentityMapper;
    private final UserMapper userMapper;
    private final TenantUserExtensionMapper tenantUserExtensionMapper;
    private final LoginAuditMapper loginAuditMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final NacosConfig nacosConfig;
    private final Executor loginAuditExecutor;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginServiceImpl(List<LoginAuthenticator> loginAuthenticators, UserIdentityMapper userIdentityMapper,
                            UserMapper userMapper, TenantUserExtensionMapper tenantUserExtensionMapper,
                            LoginAuditMapper loginAuditMapper, StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper, NacosConfig nacosConfig,
                            @Qualifier("loginAuditExecutor") Executor loginAuditExecutor) {
        this.loginAuthenticatorMap = loginAuthenticators.stream()
                .collect(Collectors.toMap(authenticator -> authenticator.supports().getValue(), Function.identity()));
        this.userIdentityMapper = userIdentityMapper;
        this.userMapper = userMapper;
        this.tenantUserExtensionMapper = tenantUserExtensionMapper;
        this.loginAuditMapper = loginAuditMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.nacosConfig = nacosConfig;
        this.loginAuditExecutor = loginAuditExecutor;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        // 1. 参数校验
        LoginTypeEnum loginType = LoginTypeEnum.fromValue(command.getLoginType());
        command.setLoginType(loginType.getValue());
        // 2. 身份认证
        AuthenticatedIdentity authenticatedIdentity = findAuthenticator(loginType).authenticate(command);
        String userId = authenticatedIdentity.getUserId();
        if (userId == null) {
            UserIdentityEntity identity = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                    .eq(UserIdentityEntity::getIdentityType, authenticatedIdentity.getIdentityType())
                    .eq(UserIdentityEntity::getIdentityProvider, authenticatedIdentity.getIdentityProvider())
                    .eq(UserIdentityEntity::getIdentityValue, authenticatedIdentity.getIdentityValue()));
            if (identity == null) {
                throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
            }
            userId = identity.getUserId();
        }
        // 3. 账号状态校验
        ensureUserActive(userId);
        // 4. 租户校验
        String tenantId = tenantUserExtensionMapper.selectPersonalTenantId(userId);
        if (tenantId == null) {
            throw new BizException(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND);
        }
        String sessionId = IdWorker.getIdStr();
        String sessionToken = newSessionToken();
        String tokenHash = sha256(sessionToken);
        SessionInfo session = newSession(sessionId, command);
        String oldSessionId = replaceDeviceSession(userId, command.getDeviceId(), sessionId);
        try {
            stringRedisTemplate.opsForValue().set(sessionKey(tokenHash), sessionValue(session, userId, tenantId), SESSION_TTL);
            stringRedisTemplate.opsForValue().set(sessionIdKey(sessionId), tokenHash, SESSION_TTL);
        } catch (RuntimeException exception) {
            rollbackDeviceSession(userId, command.getDeviceId(), sessionId, oldSessionId);
            throw exception;
        }
        if (!oldSessionId.isEmpty()) {
            deleteSessionValues(oldSessionId);
        }
        writeLoginAuditAsync(userId, tenantId, command, authenticatedIdentity);
        // 8. 构造返回结果
        LoginResult result = new LoginResult();
        result.setSessionToken(sessionToken);
        result.setSessionId(sessionId);
        result.setUserId(userId);
        result.setTenantId(tenantId);
        return result;
    }

    private String replaceDeviceSession(String userId, String deviceId, String sessionId) {
        String result = stringRedisTemplate.execute(REPLACE_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, String.valueOf(maxDevices()), sessionId, String.valueOf(SESSION_TTL.toMillis()));
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

    private void ensureUserActive(String userId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, userId)
                .eq(UserEntity::getStatus, 1));
        if (user == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
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
            loginAuditExecutor.execute(() -> {
                try {
                    loginAuditMapper.insert(audit);
                } catch (Exception exception) {
                    log.error("[LoginAudit] 写入登录审计失败, userId={}", userId, exception);
                }
            });
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


    private SessionInfo newSession(String sessionId, LoginCommand command) {
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
        return "ai-admin:ai-base:auth:session:{" + tokenHash + "}";
    }

    private String sessionIdKey(String sessionId) {
        return "ai-admin:ai-base:auth:session-id:{" + sessionId + "}";
    }

    private String userSessionsKey(String userId) {
        return "ai-admin:ai-base:auth:user-sessions:{" + userId + "}";
    }

    private String sessionValue(SessionInfo info, String userId, String tenantId) {
        try {
            return objectMapper.writeValueAsString(new SessionData(info, userId, tenantId));
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
                               String loginIp, String userAgent, String loginAt) {
        private SessionData(SessionInfo info, String userId, String tenantId) {
            this(info.getSessionId(), userId, tenantId, info.getDeviceId(), info.getLoginIp(), info.getUserAgent(),
                    info.getLoginAt() == null ? null : info.getLoginAt().toString());
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

