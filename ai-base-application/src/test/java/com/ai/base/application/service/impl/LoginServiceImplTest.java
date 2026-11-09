package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.LoginResult;
import com.ai.base.application.auth.SessionInfo;
import com.ai.base.application.bo.UserAccountBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.bo.UserIdentityBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.model.LoginAuditEntry;
import com.ai.base.application.model.SessionRecord;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.port.AuthPolicyPort;
import com.ai.base.application.port.LoginAuditPort;
import com.ai.base.application.port.SessionStore;
import com.ai.base.application.port.UserAccountPort;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserTenantPreferenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginServiceImplTest {
    private static final String USER_ID = "user-1";
    private static final String SESSION_ID = "session-1";
    private static final String TOKEN_HASH = "token-hash";
    private static final String DEVICE_ID = "device-0000000001";

    private SessionStore sessionStore;
    private AuthPolicyPort authPolicyPort;
    private LoginAuditPort loginAuditPort;
    /** 捕获式替身：收集提交的审计任务，测试手动执行以模拟异步调度 */
    private final List<Runnable> submittedAuditTasks = new ArrayList<>();
    private Executor loginAuditExecutor;
    private LoginServiceImpl loginService;
    private TenantUserService tenantUserService;
    private UserTenantPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        sessionStore = mock(SessionStore.class);
        authPolicyPort = mock(AuthPolicyPort.class);
        loginAuditPort = mock(LoginAuditPort.class);
        submittedAuditTasks.clear();
        loginAuditExecutor = submittedAuditTasks::add;
        tenantUserService = mock(TenantUserService.class);
        preferenceService = mock(UserTenantPreferenceService.class);
        when(authPolicyPort.authConfig()).thenReturn(baseAuthConfig(5));
        when(sessionStore.replaceDeviceSlot(anyString(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(new SessionStore.DeviceSlot("", false));
        loginService = new LoginServiceImpl(List.of(),
                sessionStore, authPolicyPort, loginAuditPort, loginAuditExecutor, tenantUserService, preferenceService);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", SESSION_ID, "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void logoutDeletesOwnedSessionAndIndexes() {
        stubSession(SESSION_ID, TOKEN_HASH, record(USER_ID, SESSION_ID, "10.0.0.1"));

        loginService.logout(SESSION_ID);

        verify(sessionStore).deleteSession(TOKEN_HASH, SESSION_ID);
        verify(sessionStore).removeDevice(USER_ID, DEVICE_ID);
    }

    @Test
    void logoutDoesNotDeleteAnotherUsersSession() {
        stubSession(SESSION_ID, TOKEN_HASH, record("user-2", SESSION_ID, "10.0.0.1"));

        loginService.logout();

        verify(sessionStore, never()).deleteSession(TOKEN_HASH, SESSION_ID);
        verify(sessionStore, never()).removeDevice(USER_ID, DEVICE_ID);
    }

    @Test
    void logoutAllInvalidatesEveryOwnedSession() {
        when(sessionStore.listSessionIds(USER_ID)).thenReturn(List.of("session-1", "session-2"));
        stubSession("session-1", "token-1", record(USER_ID, "session-1", "10.0.0.1"));
        stubSession("session-2", "token-2", record(USER_ID, "session-2", "10.0.0.2"));

        loginService.logoutAll();

        verify(sessionStore).deleteSession("token-1", "session-1");
        verify(sessionStore).deleteSession("token-2", "session-2");
    }

    @Test
    void listSessionsReturnsOnlyOwnedActiveSessions() {
        when(sessionStore.listSessionIds(USER_ID)).thenReturn(List.of("session-1", "session-2"));
        stubSession("session-1", "token-1", record(USER_ID, "session-1", "10.0.0.1"));
        stubSession("session-2", "token-2", null);

        List<SessionInfo> sessions = loginService.listSessions();

        assertThat(sessions).singleElement().satisfies(session -> {
            assertThat(session.getSessionId()).isEqualTo("session-1");
            assertThat(session.isCurrent()).isTrue();
            assertThat(session.getLoginIp()).isEqualTo("10.0.0.1");
        });
        verify(sessionStore, never()).removeDevice(anyString(), anyString());
    }

    @Test
    void loginWritesNacosSessionPolicy() throws Exception {
        LoginAuthenticator authenticator = mock(LoginAuthenticator.class);
        when(authenticator.supports()).thenReturn(LoginTypeEnum.ACCOUNT_PASSWORD);
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(USER_ID);
        identity.setIdentityValue("13800000000");
        when(authenticator.authenticate(any(LoginCommand.class))).thenReturn(identity);
        when(tenantUserService.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-1"));
        when(authPolicyPort.authConfig()).thenReturn(baseAuthConfig(5, 120, 30));

        loginService = new LoginServiceImpl(List.of(authenticator),
                sessionStore, authPolicyPort, loginAuditPort, loginAuditExecutor, tenantUserService, preferenceService);
        LoginCommand command = new LoginCommand();
        command.setLoginType(LoginTypeEnum.ACCOUNT_PASSWORD.getValue());
        command.setDeviceId(DEVICE_ID);

        var result = loginService.login(command);
        assertThat(LoginResult.class.getDeclaredFields()).extracting("name")
                .containsExactly("sessionToken", "tenantId");
        assertThat(result.getSessionToken()).isNotBlank();
        assertThat(result.getTenantId()).isEqualTo("tenant-1");

        org.mockito.ArgumentCaptor<SessionRecord> record = org.mockito.ArgumentCaptor.forClass(SessionRecord.class);
        verify(sessionStore).saveSession(record.capture(), anyString());
        assertThat(record.getValue().userId()).isEqualTo(USER_ID);
        assertThat(record.getValue().sessionId()).isNotBlank();
        assertThat(record.getValue().tenantId()).isEqualTo("tenant-1");
        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-1"),
                eq(record.getValue().loginAt().truncatedTo(ChronoUnit.MICROS)));
        assertThat(record.getValue().ttlMillis()).isEqualTo(120 * 60 * 1000L);
        assertThat(record.getValue().renewWindowMillis()).isEqualTo(30 * 60 * 1000L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void loginAuditPropagatesAndClearsTraceContext(boolean recordFails) throws Exception {
        LoginCommand command = prepareLogin(DEVICE_ID);
        when(tenantUserService.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-1"));
        RequestContext context = RequestContextHolder.current().orElseThrow();
        doAnswer(invocation -> {
            assertThat(RequestContextHolder.current()).contains(context);
            assertThat(MDC.get("traceId")).isEqualTo(context.traceId());
            assertThat(MDC.get("callChain")).isEqualTo(context.callChain());
            if (recordFails) {
                throw new IllegalStateException("audit insert failed");
            }
            return null;
        }).when(loginAuditPort).record(any(LoginAuditEntry.class));
        var executor = Executors.newSingleThreadExecutor();
        try {
            MDC.put("traceId", context.traceId());
            MDC.put("callChain", context.callChain());
            loginService.login(command);
            assertThat(submittedAuditTasks).hasSize(1);
            Runnable auditTask = submittedAuditTasks.get(0);
            RequestContextHolder.clear();
            MDC.clear();
            executor.submit(() -> {
                auditTask.run();
                assertThat(RequestContextHolder.current()).isEmpty();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }).get(5, TimeUnit.SECONDS);
            verify(loginAuditPort).record(any(LoginAuditEntry.class));
        } finally {
            executor.shutdownNow();
            MDC.clear();
        }
    }

    @Test
    void loginRejectsNewDeviceWhenLimitReached() {
        LoginAuthenticator authenticator = mock(LoginAuthenticator.class);
        when(authenticator.supports()).thenReturn(LoginTypeEnum.ACCOUNT_PASSWORD);
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(USER_ID);
        identity.setIdentityValue("13800000000");
        when(authenticator.authenticate(any(LoginCommand.class))).thenReturn(identity);
        when(tenantUserService.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-1"));
        when(authPolicyPort.authConfig()).thenReturn(baseAuthConfig(1));
        when(sessionStore.replaceDeviceSlot(anyString(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(new SessionStore.DeviceSlot(null, true));

        LoginCommand command = new LoginCommand();
        command.setLoginType(LoginTypeEnum.ACCOUNT_PASSWORD.getValue());
        command.setDeviceId("device-0000000002");

        assertThatThrownBy(() -> new LoginServiceImpl(List.of(authenticator),
                sessionStore, authPolicyPort, loginAuditPort, loginAuditExecutor, tenantUserService, preferenceService)
                .login(command))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.DEVICE_LIMIT_EXCEEDED));
        verify(loginAuditPort, never()).record(any());
    }

    @Test
    void smsAuthenticationResolvesBoundUserAfterVerifyingCode() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserAccountPort userAccountPort = mock(UserAccountPort.class);
        when(userAccountPort.findIdentity(IdentityTypeEnum.MOBILE.getValue(),
                IdentityProviderEnum.SMS.getValue(), "13800000000"))
                .thenReturn(Optional.of(identity(USER_ID)));
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.of(account(USER_ID)));

        AuthenticatedIdentity identity = new SmsLoginAuthenticatorImpl(smsCodeService, userAccountPort)
                .authenticate(smsCommand());

        assertThat(identity.getUserId()).isEqualTo(USER_ID);
        assertThat(identity.getIdentityType()).isEqualTo(IdentityTypeEnum.MOBILE.getValue());
        assertThat(identity.getIdentityProvider()).isEqualTo(IdentityProviderEnum.SMS.getValue());
        assertThat(identity.getIdentityValue()).isEqualTo("13800000000");
        assertThat(identity.getMobile()).isEqualTo("13800000000");
        var order = inOrder(smsCodeService, userAccountPort);
        order.verify(smsCodeService).consumeLoginCode("13800000000", "123456");
        order.verify(userAccountPort).findIdentity(IdentityTypeEnum.MOBILE.getValue(),
                IdentityProviderEnum.SMS.getValue(), "13800000000");
        order.verify(userAccountPort).findActiveUser(USER_ID);
        order.verifyNoMoreInteractions();
    }

    @Test
    void smsAuthenticationRejectsUnboundIdentity() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserAccountPort userAccountPort = mock(UserAccountPort.class);
        when(userAccountPort.findIdentity(anyString(), anyString(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new SmsLoginAuthenticatorImpl(smsCodeService, userAccountPort).authenticate(smsCommand()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED));

        verify(userAccountPort, never()).findActiveUser(anyString());
    }

    @Test
    void smsAuthenticationDoesNotResolveUserWhenCodeInvalid() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserAccountPort userAccountPort = mock(UserAccountPort.class);
        doThrow(new BizException(ErrorCodeEnum.SMS_CODE_INVALID))
                .when(smsCodeService).consumeLoginCode("13800000000", "123456");

        assertThatThrownBy(() -> new SmsLoginAuthenticatorImpl(smsCodeService, userAccountPort).authenticate(smsCommand()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SMS_CODE_INVALID));

        verifyNoInteractions(userAccountPort);
    }

    @Test
    void smsAuthenticationRejectsUnavailableUser() {
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        UserAccountPort userAccountPort = mock(UserAccountPort.class);
        when(userAccountPort.findIdentity(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(identity(USER_ID)));
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new SmsLoginAuthenticatorImpl(smsCodeService, userAccountPort)
                .authenticate(smsCommand()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED));

        verify(userAccountPort).findActiveUser(USER_ID);
    }

    @Test
    void passwordAuthenticationResolvesActiveUserWithSingleUserQuery() {
        UserAccountPort userAccountPort = mock(UserAccountPort.class);
        when(userAccountPort.findIdentity(IdentityTypeEnum.ACCOUNT.getValue(),
                IdentityProviderEnum.LOCAL.getValue(), "account"))
                .thenReturn(Optional.of(identity(USER_ID)));
        UserAccountBO account = account(USER_ID);
        account.setPasswordHash(new BCryptPasswordEncoder().encode("password"));
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.of(account));
        LoginCommand command = new LoginCommand();
        command.setAccount("account");
        command.setPassword("password");

        assertThat(new PasswordLoginAuthenticatorImpl(userAccountPort).authenticate(command).getUserId())
                .isEqualTo(USER_ID);

        verify(userAccountPort).findIdentity(IdentityTypeEnum.ACCOUNT.getValue(),
                IdentityProviderEnum.LOCAL.getValue(), "account");
        verify(userAccountPort).findActiveUser(USER_ID);
        verifyNoMoreInteractions(userAccountPort);
    }

    @Test
    void passwordAuthenticationRejectsUnavailableUser() {
        UserAccountPort userAccountPort = mock(UserAccountPort.class);
        when(userAccountPort.findIdentity(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(identity(USER_ID)));
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.empty());
        LoginCommand command = new LoginCommand();
        command.setAccount("account");
        command.setPassword("password");

        assertThatThrownBy(() -> new PasswordLoginAuthenticatorImpl(userAccountPort).authenticate(command))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.LOGIN_FAILED));

        verify(userAccountPort).findActiveUser(USER_ID);
    }

    @Test
    void loginPrefersAvailableDefaultOverLastUsed() {
        LoginCommand command = prepareLogin(DEVICE_ID);
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference("tenant-default", "tenant-last"));
        when(tenantUserService.findAvailable(USER_ID, "tenant-default")).thenReturn(Optional.of(new UserTenantBO()));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-default");

        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-default"), any(LocalDateTime.class));
        verify(tenantUserService, never()).findAvailable(USER_ID, "tenant-last");
        verify(tenantUserService, never()).findPersonalTenantId(USER_ID);
    }

    @Test
    void loginFallsBackFromUnavailableDefaultToLastUsed() {
        LoginCommand command = prepareLogin(DEVICE_ID);
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference("tenant-removed", "tenant-last"));
        when(tenantUserService.findAvailable(USER_ID, "tenant-last")).thenReturn(Optional.of(new UserTenantBO()));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-last");

        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-last"), any(LocalDateTime.class));
        verify(tenantUserService, never()).findPersonalTenantId(USER_ID);
    }

    @Test
    void loginUsesLastUsedWhenDefaultUnset() {
        LoginCommand command = prepareLogin(DEVICE_ID);
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference(null, "tenant-last"));
        when(tenantUserService.findAvailable(USER_ID, "tenant-last")).thenReturn(Optional.of(new UserTenantBO()));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-last");

        verify(tenantUserService).findAvailable(USER_ID, "tenant-last");
        verify(tenantUserService, never()).findAvailable(USER_ID, null);
        verify(tenantUserService, never()).findPersonalTenantId(USER_ID);
    }

    @Test
    void loginFallsBackToPersonalWhenBothPreferencesUnavailable() {
        LoginCommand command = prepareLogin(DEVICE_ID);
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference("tenant-removed", "tenant-disabled"));
        when(tenantUserService.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-personal"));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-personal");

        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-personal"), any(LocalDateTime.class));
    }

    @Test
    void loginSkipsUnsetPreferenceFields() {
        LoginCommand command = prepareLogin(DEVICE_ID);
        when(preferenceService.getByUserId(USER_ID)).thenReturn(preference(null, null));
        when(tenantUserService.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-personal"));

        assertThat(loginService.login(command).getTenantId()).isEqualTo("tenant-personal");

        verify(tenantUserService, never()).findAvailable(anyString(), any());
    }

    @Test
    void loginRejectsWhenNoAvailableSpaceAndDoesNotCreateSession() {
        LoginCommand command = prepareLogin(DEVICE_ID);

        assertThatThrownBy(() -> loginService.login(command))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PERSONAL_TENANT_NOT_FOUND));

        verifyNoInteractions(sessionStore);
        verify(preferenceService, never()).recordLastUsed(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void loginCreatesIndependentTokensForDifferentDevices() {
        LoginCommand first = prepareLogin(DEVICE_ID);
        when(tenantUserService.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-1"));
        LoginCommand second = new LoginCommand();
        second.setLoginType(first.getLoginType());
        second.setDeviceId("device-0000000002");

        var phone = loginService.login(first);
        var computer = loginService.login(second);

        assertThat(phone.getSessionToken()).isNotEqualTo(computer.getSessionToken());
        org.mockito.ArgumentCaptor<String> tokenHashes = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sessionStore, times(2)).saveSession(any(SessionRecord.class), tokenHashes.capture());
        assertThat(tokenHashes.getAllValues()).doesNotHaveDuplicates();
        verify(sessionStore, never()).deleteSession(anyString(), anyString());
    }

    @Test
    void switchUpdatesOnlyCurrentSessionAndRecordsLastUsed() {
        UserTenantBO tenant = prepareSwitch(SessionStore.TenantSwitchResult.OK);

        assertThat(loginService.switchTenant("tenant-2")).isSameAs(tenant);

        verify(sessionStore).switchTenant(TOKEN_HASH, USER_ID, SESSION_ID, "tenant-1", "tenant-2");
        verify(preferenceService).recordLastUsed(eq(USER_ID), eq("tenant-2"), any(LocalDateTime.class));
        verify(sessionStore, never()).deleteSession(anyString(), anyString());
        verify(sessionStore, never()).saveSession(any(SessionRecord.class), anyString());
    }

    @Test
    void switchRecordsTimeCapturedBeforeStoreUpdateRatherThanPersistenceTime() {
        prepareSwitch(SessionStore.TenantSwitchResult.OK);
        LocalDateTime switchTime = LocalDateTime.of(2026, 10, 27, 10, 0, 0, 123456789);
        LocalDateTime delayedTime = switchTime.plusSeconds(5);
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(switchTime, delayedTime);
            when(sessionStore.switchTenant(TOKEN_HASH, USER_ID, SESSION_ID, "tenant-1", "tenant-2"))
                    .thenAnswer(invocation -> {
                        assertThat(LocalDateTime.now()).isEqualTo(delayedTime);
                        return SessionStore.TenantSwitchResult.OK;
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

        verifyNoInteractions(sessionStore, preferenceService);
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
        prepareSwitch(SessionStore.TenantSwitchResult.SESSION_INVALID);

        assertThatThrownBy(() -> loginService.switchTenant("tenant-2"))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SESSION_INVALID));

        verifyNoInteractions(preferenceService);
    }

    @Test
    void switchRejectsConcurrentTenantChange() {
        prepareSwitch(SessionStore.TenantSwitchResult.TENANT_MISMATCH);

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
        verifyNoInteractions(tenantUserService, sessionStore, preferenceService);
    }

    private void stubSession(String sessionId, String tokenHash, SessionRecord record) {
        if (tokenHash == null) {
            when(sessionStore.findTokenHash(sessionId)).thenReturn(Optional.empty());
            return;
        }
        when(sessionStore.findTokenHash(sessionId)).thenReturn(Optional.of(tokenHash));
        if (record == null) {
            when(sessionStore.findSession(tokenHash)).thenReturn(Optional.empty());
        } else {
            when(sessionStore.findSession(tokenHash)).thenReturn(Optional.of(record));
        }
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
                sessionStore, authPolicyPort, loginAuditPort, loginAuditExecutor, tenantUserService, preferenceService);
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

    private UserIdentityBO identity(String userId) {
        UserIdentityBO result = new UserIdentityBO();
        result.setUserId(userId);
        return result;
    }

    private UserAccountBO account(String userId) {
        UserAccountBO result = new UserAccountBO();
        result.setUserId(userId);
        return result;
    }

    private SessionRecord record(String userId, String sessionId, String loginIp) {
        return new SessionRecord(sessionId, userId, "tenant-1", DEVICE_ID, loginIp, "JUnit",
                LocalDateTime.of(2026, 9, 22, 12, 0), 4 * 60 * 60 * 1000L, 10 * 60 * 1000L);
    }

    private UserTenantBO prepareSwitch(SessionStore.TenantSwitchResult result) {
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId("tenant-2");
        when(tenantUserService.requireAvailable(USER_ID, "tenant-2")).thenReturn(tenant);
        when(sessionStore.findTokenHash(SESSION_ID)).thenReturn(Optional.of(TOKEN_HASH));
        when(sessionStore.switchTenant(TOKEN_HASH, USER_ID, SESSION_ID, "tenant-1", "tenant-2"))
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
}
