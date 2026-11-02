package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.SessionInfo;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.infrastructure.config.NacosConfig;
import com.ai.base.infrastructure.enums.NacosDataIdEnum;
import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import com.ai.base.infrastructure.persistence.entity.LoginAuditEntity;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.mapper.LoginAuditMapper;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private TenantUserService tenantUserService;
    private UserTenantPreferenceService preferenceService;

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
        tenantUserService = mock(TenantUserService.class);
        preferenceService = mock(UserTenantPreferenceService.class);
        when(nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class))
                .thenReturn(baseAuthConfig(5));
        loginService = new LoginServiceImpl(List.of(),
                tenantUserExtensionMapper, loginAuditMapper, redisTemplate,
                new ObjectMapper().findAndRegisterModules(), nacosConfig, loginAuditExecutor, tenantUserService, preferenceService);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", SESSION_ID, "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void logoutDeletesOwnedSessionAndIndexes() throws Exception {
        when(valueOperations.get("ai-base:auth:session-id:{" + SESSION_ID + "}")).thenReturn(TOKEN_HASH);
        when(valueOperations.get("ai-base:auth:session:{" + TOKEN_HASH + "}"))
                .thenReturn(sessionJson(USER_ID, SESSION_ID, "10.0.0.1"));

        loginService.logout(SESSION_ID);

        verify(redisTemplate).delete("ai-base:auth:session:{" + TOKEN_HASH + "}");
        verify(redisTemplate).delete("ai-base:auth:session-id:{" + SESSION_ID + "}");
        verify(hashOperations).delete("ai-base:auth:user-sessions:{" + USER_ID + "}", "device-0000000001");
    }

    @Test
    void logoutDoesNotDeleteAnotherUsersSession() throws Exception {
        when(valueOperations.get("ai-base:auth:session-id:{" + SESSION_ID + "}")).thenReturn(TOKEN_HASH);
        when(valueOperations.get("ai-base:auth:session:{" + TOKEN_HASH + "}"))
                .thenReturn(sessionJson("user-2", SESSION_ID, "10.0.0.1"));

        loginService.logout();

        verify(redisTemplate, never()).delete("ai-base:auth:session:{" + TOKEN_HASH + "}");
        verify(redisTemplate, never()).delete("ai-base:auth:session-id:{" + SESSION_ID + "}");
        verify(hashOperations, never()).delete("ai-base:auth:user-sessions:{" + USER_ID + "}", "device-0000000001");
    }

    @Test
    void logoutAllInvalidatesEveryOwnedSession() throws Exception {
        when(hashOperations.values("ai-base:auth:user-sessions:{" + USER_ID + "}"))
                .thenReturn(List.of("session-1", "session-2"));
        when(valueOperations.get("ai-base:auth:session-id:{session-1}")).thenReturn("token-1");
        when(valueOperations.get("ai-base:auth:session-id:{session-2}")).thenReturn("token-2");
        when(valueOperations.get("ai-base:auth:session:{token-1}"))
                .thenReturn(sessionJson(USER_ID, "session-1", "10.0.0.1"));
        when(valueOperations.get("ai-base:auth:session:{token-2}"))
                .thenReturn(sessionJson(USER_ID, "session-2", "10.0.0.2"));

        loginService.logoutAll();

        verify(redisTemplate).delete("ai-base:auth:session:{token-1}");
        verify(redisTemplate).delete("ai-base:auth:session-id:{session-1}");
        verify(redisTemplate).delete("ai-base:auth:session:{token-2}");
        verify(redisTemplate).delete("ai-base:auth:session-id:{session-2}");
    }

    @Test
    void listSessionsReturnsOnlyOwnedActiveSessions() throws Exception {
        when(hashOperations.values("ai-base:auth:user-sessions:{" + USER_ID + "}"))
                .thenReturn(List.of("session-1", "session-2"));
        when(valueOperations.get("ai-base:auth:session-id:{session-1}")).thenReturn("token-1");
        when(valueOperations.get("ai-base:auth:session-id:{session-2}")).thenReturn("token-2");
        when(valueOperations.get("ai-base:auth:session:{token-1}"))
                .thenReturn(sessionJson(USER_ID, "session-1", "10.0.0.1"));
        when(valueOperations.get("ai-base:auth:session:{token-2}")).thenReturn(null);

        List<SessionInfo> sessions = loginService.listSessions();

        assertThat(sessions).singleElement().satisfies(session -> {
            assertThat(session.getSessionId()).isEqualTo("session-1");
            assertThat(session.isCurrent()).isTrue();
            assertThat(session.getLoginIp()).isEqualTo("10.0.0.1");
        });
        verify(hashOperations, never()).delete("ai-base:auth:user-sessions:{" + USER_ID + "}", "device-0000000001");
    }

    @Test
    void loginWritesNacosSessionPolicy() throws Exception {
        LoginAuthenticator authenticator = mock(LoginAuthenticator.class);
        when(authenticator.supports()).thenReturn(LoginTypeEnum.ACCOUNT_PASSWORD);
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(USER_ID);
        identity.setIdentityValue("13800000000");
        when(authenticator.authenticate(org.mockito.ArgumentMatchers.any(LoginCommand.class))).thenReturn(identity);
        when(tenantUserExtensionMapper.selectPersonalTenantId(USER_ID)).thenReturn("tenant-1");
        when(nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class))
                .thenReturn(baseAuthConfig(5, 120, 30));

        LoginCommand command = new LoginCommand();
        command.setLoginType(LoginTypeEnum.ACCOUNT_PASSWORD.getValue());
        command.setDeviceId("device-0000000001");

        loginService = new LoginServiceImpl(List.of(authenticator),
                tenantUserExtensionMapper, loginAuditMapper, redisTemplate,
                new ObjectMapper().findAndRegisterModules(), nacosConfig, loginAuditExecutor, tenantUserService, preferenceService);
        var result = loginService.login(command);
        JsonNode response = new ObjectMapper().valueToTree(result);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.path("sessionToken").textValue()).isNotBlank();
        assertThat(response.path("tenantId").textValue()).isEqualTo("tenant-1");
        assertThat(response.has("sessionId")).isFalse();
        assertThat(response.has("userId")).isFalse();

        org.mockito.ArgumentCaptor<String> sessionValue = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.startsWith("ai-base:auth:session:{"),
                sessionValue.capture(), eq(Duration.ofMinutes(120)));
        JsonNode session = new ObjectMapper().readTree(sessionValue.getValue());
        assertThat(session.path("userId").textValue()).isEqualTo(USER_ID);
        assertThat(session.path("sessionId").textValue()).isNotBlank();
        assertThat(session.has("clientType")).isFalse();
        assertThat(session.path("tenantId").textValue()).isEqualTo("tenant-1");
        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-1"),
                eq(LocalDateTime.parse(session.path("loginAt").textValue()).truncatedTo(ChronoUnit.MICROS)));
        assertThat(session.path("ttlMillis").longValue()).isEqualTo(Duration.ofMinutes(120).toMillis());
        assertThat(session.path("renewWindowMillis").longValue()).isEqualTo(Duration.ofMinutes(30).toMillis());
        verifyNoInteractions(userMapper);
    }

    @Test
    void loginRejectsNewDeviceWhenLimitReached() throws Exception {
        LoginAuthenticator authenticator = mock(LoginAuthenticator.class);
        when(authenticator.supports()).thenReturn(LoginTypeEnum.ACCOUNT_PASSWORD);
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(USER_ID);
        identity.setIdentityValue("13800000000");
        when(authenticator.authenticate(org.mockito.ArgumentMatchers.any(LoginCommand.class))).thenReturn(identity);
        when(tenantUserExtensionMapper.selectPersonalTenantId(USER_ID)).thenReturn("tenant-1");
        when(nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class))
                .thenReturn(baseAuthConfig(1));
        when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisScript<String>>any(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn("DEVICE_LIMIT_EXCEEDED");

        LoginCommand command = new LoginCommand();
        command.setLoginType(LoginTypeEnum.ACCOUNT_PASSWORD.getValue());
        command.setDeviceId("device-0000000002");

        assertThatThrownBy(() -> new LoginServiceImpl(List.of(authenticator),
                tenantUserExtensionMapper, loginAuditMapper, redisTemplate, new ObjectMapper(), nacosConfig, loginAuditExecutor, tenantUserService, preferenceService)
                .login(command))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED));
        verify(loginAuditMapper, never()).insert(org.mockito.ArgumentMatchers.<LoginAuditEntity>any());
    }

    @Test
    void smsAuthenticationResolvesBoundUserAfterVerifyingCode() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserIdentityMapper identityMapper = mock(UserIdentityMapper.class);
        UserIdentityEntity binding = new UserIdentityEntity();
        binding.setUserId(USER_ID);
        when(identityMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserIdentityEntity>>any()))
                .thenReturn(binding);
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserEntity>>any()))
                .thenReturn(new UserEntity());

        AuthenticatedIdentity identity = new SmsLoginAuthenticatorImpl(smsCodeService, identityMapper, userMapper)
                .authenticate(smsCommand());

        assertThat(identity.getUserId()).isEqualTo(USER_ID);
        assertThat(identity.getIdentityType()).isEqualTo(IdentityTypeEnum.MOBILE.getValue());
        assertThat(identity.getIdentityProvider()).isEqualTo(IdentityProviderEnum.SMS.getValue());
        assertThat(identity.getIdentityValue()).isEqualTo("13800000000");
        assertThat(identity.getMobile()).isEqualTo("13800000000");
        var order = inOrder(smsCodeService, identityMapper, userMapper);
        order.verify(smsCodeService).consumeLoginCode("13800000000", "123456");
        order.verify(identityMapper).selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserIdentityEntity>>any());
        order.verify(userMapper).selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserEntity>>any());
        order.verifyNoMoreInteractions();
        assertActiveUserQuery();
    }

    @Test
    void smsAuthenticationRejectsUnboundIdentity() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserIdentityMapper identityMapper = mock(UserIdentityMapper.class);

        assertThatThrownBy(() -> new SmsLoginAuthenticatorImpl(smsCodeService, identityMapper, userMapper).authenticate(smsCommand()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED));

        verify(smsCodeService).consumeLoginCode("13800000000", "123456");
        verify(identityMapper).selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserIdentityEntity>>any());
        verifyNoInteractions(userMapper);
    }

    @Test
    void smsAuthenticationDoesNotResolveUserWhenCodeInvalid() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserIdentityMapper identityMapper = mock(UserIdentityMapper.class);
        doThrow(new BizException(ErrorCodeEnum.SMS_CODE_INVALID))
                .when(smsCodeService).consumeLoginCode("13800000000", "123456");

        assertThatThrownBy(() -> new SmsLoginAuthenticatorImpl(smsCodeService, identityMapper, userMapper).authenticate(smsCommand()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_CODE_INVALID));

        verifyNoInteractions(identityMapper, userMapper);
    }

    @Test
    void smsAuthenticationRejectsUnavailableUser() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserIdentityMapper identityMapper = mock(UserIdentityMapper.class);
        UserIdentityEntity binding = new UserIdentityEntity();
        binding.setUserId(USER_ID);
        when(identityMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserIdentityEntity>>any()))
                .thenReturn(binding);

        assertThatThrownBy(() -> new SmsLoginAuthenticatorImpl(smsCodeService, identityMapper, userMapper)
                .authenticate(smsCommand()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED));

        assertActiveUserQuery();
    }

    @Test
    void passwordAuthenticationResolvesActiveUserWithSingleUserQuery() {
        UserIdentityMapper identityMapper = mock(UserIdentityMapper.class);
        UserIdentityEntity binding = new UserIdentityEntity();
        binding.setUserId(USER_ID);
        when(identityMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserIdentityEntity>>any()))
                .thenReturn(binding);
        UserEntity user = new UserEntity();
        user.setPasswordHash(new BCryptPasswordEncoder().encode("password"));
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserEntity>>any())).thenReturn(user);
        LoginCommand command = new LoginCommand();
        command.setAccount("account");
        command.setPassword("password");

        assertThat(new PasswordLoginAuthenticatorImpl(identityMapper, userMapper).authenticate(command).getUserId())
                .isEqualTo(USER_ID);

        assertActiveUserQuery();
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void passwordAuthenticationRejectsUnavailableUser() {
        UserIdentityMapper identityMapper = mock(UserIdentityMapper.class);
        UserIdentityEntity binding = new UserIdentityEntity();
        binding.setUserId(USER_ID);
        when(identityMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<UserIdentityEntity>>any()))
                .thenReturn(binding);
        LoginCommand command = new LoginCommand();
        command.setAccount("account");
        command.setPassword("password");

        assertThatThrownBy(() -> new PasswordLoginAuthenticatorImpl(identityMapper, userMapper).authenticate(command))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED));

        assertActiveUserQuery();
    }

    @Test
    void loginPrefersAvailableDefaultOverLastUsed() {
        LoginCommand command = prepareLogin("device-0000000001");
        UserTenantPreferenceBO preference = preference("tenant-default", "tenant-last");
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference);
        when(tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(USER_ID, "tenant-default"))
                .thenReturn(tenantRow("tenant-default"));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-default");

        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-default"), any(LocalDateTime.class));
        verify(tenantUserExtensionMapper, never()).selectAvailableByUserIdAndTenantId(USER_ID, "tenant-last");
        verify(tenantUserExtensionMapper, never()).selectPersonalTenantId(USER_ID);
    }

    @Test
    void loginFallsBackFromUnavailableDefaultToLastUsed() {
        LoginCommand command = prepareLogin("device-0000000001");
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference("tenant-removed", "tenant-last"));
        when(tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(USER_ID, "tenant-last"))
                .thenReturn(tenantRow("tenant-last"));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-last");

        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-last"), any(LocalDateTime.class));
        verify(tenantUserExtensionMapper, never()).selectPersonalTenantId(USER_ID);
    }

    @Test
    void loginUsesLastUsedWhenDefaultUnset() {
        LoginCommand command = prepareLogin("device-0000000001");
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference(null, "tenant-last"));
        when(tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(USER_ID, "tenant-last"))
                .thenReturn(tenantRow("tenant-last"));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-last");

        verify(tenantUserExtensionMapper).selectAvailableByUserIdAndTenantId(USER_ID, "tenant-last");
        verify(tenantUserExtensionMapper, never()).selectAvailableByUserIdAndTenantId(eq(USER_ID), isNull());
        verify(tenantUserExtensionMapper, never()).selectPersonalTenantId(USER_ID);
    }

    @Test
    void loginFallsBackToPersonalWhenBothPreferencesUnavailable() {
        LoginCommand command = prepareLogin("device-0000000001");
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference("tenant-removed", "tenant-disabled"));
        when(tenantUserExtensionMapper.selectPersonalTenantId(USER_ID)).thenReturn("tenant-personal");

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-personal");

        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-personal"), any(LocalDateTime.class));
    }

    @Test
    void loginSkipsUnsetPreferenceFields() {
        LoginCommand command = prepareLogin("device-0000000001");
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference(null, null));
        when(tenantUserExtensionMapper.selectPersonalTenantId(USER_ID)).thenReturn("tenant-personal");

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-personal");

        verify(tenantUserExtensionMapper, never()).selectAvailableByUserIdAndTenantId(anyString(), any());
    }

    @Test
    void loginRejectsWhenNoAvailableSpaceAndDoesNotCreateSession() {
        LoginCommand command = prepareLogin("device-0000000001");

        assertThatThrownBy(() -> loginService.login(command))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND));

        verifyNoInteractions(valueOperations);
        verify(preferenceService, never()).recordLastUsed(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void loginCreatesIndependentTokensForDifferentDevices() {
        LoginCommand first = prepareLogin("device-0000000001");
        when(tenantUserExtensionMapper.selectPersonalTenantId(USER_ID)).thenReturn("tenant-1");
        LoginCommand second = new LoginCommand();
        second.setLoginType(first.getLoginType());
        second.setDeviceId("device-0000000002");

        var phone = loginService.login(first);
        var computer = loginService.login(second);

        assertThat(phone.getSessionToken()).isNotEqualTo(computer.getSessionToken());
        var sessionKeys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(4)).set(sessionKeys.capture(), anyString(), any(Duration.class));
        assertThat(sessionKeys.getAllValues()).doesNotHaveDuplicates();
        verify(valueOperations, times(2)).set(startsWith("ai-base:auth:session:{"), anyString(), any(Duration.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void switchUpdatesOnlyCurrentSessionAndRecordsLastUsed() {
        UserTenantBO tenant = prepareSwitch(1L);

        assertThat(loginService.switchTenant("tenant-2")).isSameAs(tenant);

        verify(redisTemplate).execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("ai-base:auth:session:{" + TOKEN_HASH + "}")),
                eq(USER_ID), eq(SESSION_ID), eq("tenant-1"), eq("tenant-2"));
        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-2"), any(LocalDateTime.class));
        verify(redisTemplate, never()).delete(anyString());
        verifyNoInteractions(hashOperations);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void switchRecordsTimeCapturedBeforeRedisUpdateRatherThanPersistenceTime() {
        prepareSwitch(1L);
        LocalDateTime switchTime = LocalDateTime.of(2026, 10, 27, 10, 0, 0, 123456789);
        LocalDateTime delayedTime = switchTime.plusSeconds(5);
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(switchTime, delayedTime);
            when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                    anyList(), eq(USER_ID), eq(SESSION_ID), eq("tenant-1"), eq("tenant-2")))
                    .thenAnswer(invocation -> {
                        assertThat(LocalDateTime.now()).isEqualTo(delayedTime);
                        return 1L;
                    });

            loginService.switchTenant("tenant-2");

            verify(preferenceService).recordLastUsed(USER_ID, "tenant-2", switchTime.truncatedTo(ChronoUnit.MICROS));
        }
    }

    @Test
    void switchRejectsUnavailableTargetBeforeChangingSession() {
        when(tenantUserService.requireAvailable(USER_ID, "tenant-2"))
                .thenThrow(new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE));

        assertThatThrownBy(() -> loginService.switchTenant("tenant-2"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE));

        verifyNoInteractions(valueOperations, preferenceService);
    }

    @Test
    void switchRejectsMissingSessionIndex() {
        when(tenantUserService.requireAvailable(USER_ID, "tenant-2")).thenReturn(new UserTenantBO());

        assertThatThrownBy(() -> loginService.switchTenant("tenant-2"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SESSION_INVALID));

        verifyNoInteractions(preferenceService);
    }

    @Test
    void switchRejectsExpiredOrLoggedOutSession() {
        prepareSwitch(0L);

        assertThatThrownBy(() -> loginService.switchTenant("tenant-2"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SESSION_INVALID));

        verifyNoInteractions(preferenceService);
    }

    @Test
    void switchRejectsConcurrentTenantChange() {
        prepareSwitch(-1L);

        assertThatThrownBy(() -> loginService.switchTenant("tenant-2"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_CONTEXT_MISMATCH));

        verifyNoInteractions(preferenceService);
    }

    @Test
    void switchRejectsMissingTarget() {
        for (String target : new String[]{null, "", " "}) {
            assertThatThrownBy(() -> loginService.switchTenant(target))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PARAMETER_REQUIRED));
        }
        verifyNoInteractions(tenantUserService, valueOperations, preferenceService);
    }

    private void assertActiveUserQuery() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserEntity.class);
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<UserEntity>> query = org.mockito.ArgumentCaptor.captor();
        verify(userMapper).selectOne(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("user_id =", "status =");
        assertThat(query.getValue().getParamNameValuePairs()).containsValue(USER_ID).containsValue(1);
    }

    private LoginCommand smsCommand() {
        LoginCommand command = new LoginCommand();
        command.setMobile("13800000000");
        command.setSmsCode("123456");
        return command;
    }

    private LoginCommand prepareLogin(String deviceId) {
        LoginAuthenticator authenticator = mock(LoginAuthenticator.class);
        when(authenticator.supports()).thenReturn(LoginTypeEnum.ACCOUNT_PASSWORD);
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(USER_ID);
        identity.setIdentityValue("13800000000");
        when(authenticator.authenticate(any(LoginCommand.class))).thenReturn(identity);
        loginService = new LoginServiceImpl(List.of(authenticator),
                tenantUserExtensionMapper, loginAuditMapper, redisTemplate,
                new ObjectMapper().findAndRegisterModules(), nacosConfig, loginAuditExecutor, tenantUserService, preferenceService);
        LoginCommand command = new LoginCommand();
        command.setLoginType(LoginTypeEnum.ACCOUNT_PASSWORD.getValue());
        command.setDeviceId(deviceId);
        return command;
    }

    private UserTenantPreferenceBO preference(String defaultTenantId, String lastUsedTenantId) {
        UserTenantPreferenceBO result = new UserTenantPreferenceBO();
        result.setDefaultTenantId(defaultTenantId);
        result.setLastUsedTenantId(lastUsedTenantId);
        return result;
    }

    private UserTenantRow tenantRow(String tenantId) {
        UserTenantRow result = new UserTenantRow();
        result.setTenantId(tenantId);
        return result;
    }

    private UserTenantBO prepareSwitch(long result) {
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId("tenant-2");
        when(tenantUserService.requireAvailable(USER_ID, "tenant-2")).thenReturn(tenant);
        when(valueOperations.get("ai-base:auth:session-id:{" + SESSION_ID + "}"))
                .thenReturn(TOKEN_HASH);
        when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(), eq(USER_ID), eq(SESSION_ID), eq("tenant-1"), eq("tenant-2")))
                .thenReturn(result);
        return tenant;
    }

    private BaseAuthConfig baseAuthConfig(int maxDevices) {
        return baseAuthConfig(maxDevices, null, null);
    }

    private BaseAuthConfig baseAuthConfig(int maxDevices, Integer ttlMinutes, Integer renewWindowMinutes) {
        BaseAuthConfig config = new BaseAuthConfig();
        BaseAuthConfig.SessionConfig sessionConfig = new BaseAuthConfig.SessionConfig();
        sessionConfig.setMaxDevices(maxDevices);
        sessionConfig.setTtlMinutes(ttlMinutes);
        sessionConfig.setRenewWindowMinutes(renewWindowMinutes);
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

