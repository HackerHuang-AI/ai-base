package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.SessionInfo;
import com.ai.base.application.common.BizException;
import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.infrastructure.config.NacosConfig;
import com.ai.base.infrastructure.enums.NacosDataIdEnum;
import com.ai.base.infrastructure.persistence.entity.LoginAuditEntity;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.mapper.LoginAuditMapper;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginServiceImplTest {
    private static final String USER_ID = "user-1";
    private static final String SESSION_ID = "session-1";
    private static final String TOKEN_HASH = "token-hash";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private ThreadPoolTaskExecutor loginAuditExecutor;
    private LoginServiceImpl loginService;
    private NacosConfig nacosConfig;
    private UserMapper userMapper;
    private TenantUserExtensionMapper tenantUserExtensionMapper;
    private LoginAuditMapper loginAuditMapper;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        hashOperations = mock(HashOperations.class);
        loginAuditExecutor = mock(ThreadPoolTaskExecutor.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        userMapper = mock(UserMapper.class);
        tenantUserExtensionMapper = mock(TenantUserExtensionMapper.class);
        loginAuditMapper = mock(LoginAuditMapper.class);
        nacosConfig = mock(NacosConfig.class);
        when(nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class))
                .thenReturn(baseAuthConfig(5));
        loginService = new LoginServiceImpl(List.of(), mock(UserIdentityMapper.class), userMapper,
                tenantUserExtensionMapper, loginAuditMapper, redisTemplate,
                new ObjectMapper().findAndRegisterModules(), nacosConfig, loginAuditExecutor);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", SESSION_ID, "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void logoutDeletesOwnedSessionAndIndexes() throws Exception {
        when(valueOperations.get("ai-admin:ai-base:auth:session-id:{" + SESSION_ID + "}")).thenReturn(TOKEN_HASH);
        when(valueOperations.get("ai-admin:ai-base:auth:session:{" + TOKEN_HASH + "}"))
                .thenReturn(sessionJson(USER_ID, SESSION_ID, "10.0.0.1"));

        loginService.logout(SESSION_ID);

        verify(redisTemplate).delete("ai-admin:ai-base:auth:session:{" + TOKEN_HASH + "}");
        verify(redisTemplate).delete("ai-admin:ai-base:auth:session-id:{" + SESSION_ID + "}");
        verify(hashOperations).delete("ai-admin:ai-base:auth:user-sessions:{" + USER_ID + "}", "device-0000000001");
    }

    @Test
    void logoutDoesNotDeleteAnotherUsersSession() throws Exception {
        when(valueOperations.get("ai-admin:ai-base:auth:session-id:{" + SESSION_ID + "}")).thenReturn(TOKEN_HASH);
        when(valueOperations.get("ai-admin:ai-base:auth:session:{" + TOKEN_HASH + "}"))
                .thenReturn(sessionJson("user-2", SESSION_ID, "10.0.0.1"));

        loginService.logout();

        verify(redisTemplate, never()).delete("ai-admin:ai-base:auth:session:{" + TOKEN_HASH + "}");
        verify(redisTemplate, never()).delete("ai-admin:ai-base:auth:session-id:{" + SESSION_ID + "}");
        verify(hashOperations, never()).delete("ai-admin:ai-base:auth:user-sessions:{" + USER_ID + "}", "device-0000000001");
    }

    @Test
    void logoutAllInvalidatesEveryOwnedSession() throws Exception {
        when(hashOperations.values("ai-admin:ai-base:auth:user-sessions:{" + USER_ID + "}"))
                .thenReturn(List.of("session-1", "session-2"));
        when(valueOperations.get("ai-admin:ai-base:auth:session-id:{session-1}")).thenReturn("token-1");
        when(valueOperations.get("ai-admin:ai-base:auth:session-id:{session-2}")).thenReturn("token-2");
        when(valueOperations.get("ai-admin:ai-base:auth:session:{token-1}"))
                .thenReturn(sessionJson(USER_ID, "session-1", "10.0.0.1"));
        when(valueOperations.get("ai-admin:ai-base:auth:session:{token-2}"))
                .thenReturn(sessionJson(USER_ID, "session-2", "10.0.0.2"));

        loginService.logoutAll();

        verify(redisTemplate).delete("ai-admin:ai-base:auth:session:{token-1}");
        verify(redisTemplate).delete("ai-admin:ai-base:auth:session-id:{session-1}");
        verify(redisTemplate).delete("ai-admin:ai-base:auth:session:{token-2}");
        verify(redisTemplate).delete("ai-admin:ai-base:auth:session-id:{session-2}");
    }

    @Test
    void listSessionsReturnsOnlyOwnedActiveSessions() throws Exception {
        when(hashOperations.values("ai-admin:ai-base:auth:user-sessions:{" + USER_ID + "}"))
                .thenReturn(List.of("session-1", "session-2"));
        when(valueOperations.get("ai-admin:ai-base:auth:session-id:{session-1}")).thenReturn("token-1");
        when(valueOperations.get("ai-admin:ai-base:auth:session-id:{session-2}")).thenReturn("token-2");
        when(valueOperations.get("ai-admin:ai-base:auth:session:{token-1}"))
                .thenReturn(sessionJson(USER_ID, "session-1", "10.0.0.1"));
        when(valueOperations.get("ai-admin:ai-base:auth:session:{token-2}")).thenReturn(null);

        List<SessionInfo> sessions = loginService.listSessions();

        assertThat(sessions).singleElement().satisfies(session -> {
            assertThat(session.getSessionId()).isEqualTo("session-1");
            assertThat(session.isCurrent()).isTrue();
            assertThat(session.getLoginIp()).isEqualTo("10.0.0.1");
        });
        verify(hashOperations, never()).delete("ai-admin:ai-base:auth:user-sessions:{" + USER_ID + "}", "device-0000000001");
    }

    @Test
    void loginRejectsNewDeviceWhenLimitReached() throws Exception {
        LoginAuthenticator authenticator = mock(LoginAuthenticator.class);
        when(authenticator.supports()).thenReturn(LoginTypeEnum.MOCK);
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(USER_ID);
        identity.setIdentityValue("13800000000");
        when(authenticator.authenticate(org.mockito.ArgumentMatchers.any(LoginCommand.class))).thenReturn(identity);
        UserEntity user = new UserEntity();
        user.setStatus(1);
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserEntity>>any())).thenReturn(user);
        when(tenantUserExtensionMapper.selectPersonalTenantId(USER_ID)).thenReturn("tenant-1");
        when(nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class))
                .thenReturn(baseAuthConfig(1));
        when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisScript<String>>any(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn("DEVICE_LIMIT_EXCEEDED");

        LoginCommand command = new LoginCommand();
        command.setLoginType(LoginTypeEnum.MOCK.getValue());
        command.setDeviceId("device-0000000002");

        assertThatThrownBy(() -> new LoginServiceImpl(List.of(authenticator), mock(UserIdentityMapper.class), userMapper,
                tenantUserExtensionMapper, loginAuditMapper, redisTemplate, new ObjectMapper(), nacosConfig, loginAuditExecutor)
                .login(command))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED));
        verify(loginAuditMapper, never()).insert(org.mockito.ArgumentMatchers.<LoginAuditEntity>any());
    }

    private BaseAuthConfig baseAuthConfig(int maxDevices) {
        BaseAuthConfig config = new BaseAuthConfig();
        BaseAuthConfig.SessionConfig sessionConfig = new BaseAuthConfig.SessionConfig();
        sessionConfig.setMaxDevices(maxDevices);
        config.setSession(sessionConfig);
        return config;
    }

    private String sessionJson(String userId, String sessionId, String loginIp) throws Exception {
        SessionInfo session = new SessionInfo();
        session.setSessionId(sessionId);
        session.setLoginIp(loginIp);
        session.setUserAgent("JUnit");
        return new ObjectMapper().writeValueAsString(Map.of(
                "sessionId", session.getSessionId(),
                "userId", userId,
                "tenantId", "tenant-1",
                "deviceId", "device-0000000001",
                "loginIp", session.getLoginIp(),
                "userAgent", session.getUserAgent(),
                "loginAt", "2026-09-22T12:00:00"));
    }
}

