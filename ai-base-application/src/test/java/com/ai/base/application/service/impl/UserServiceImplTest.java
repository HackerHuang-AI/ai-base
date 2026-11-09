package com.ai.base.application.service.impl;

import com.ai.base.application.bo.PersonalMobileUpdateBO;
import com.ai.base.application.bo.PersonalTenantUpdateBO;
import com.ai.base.application.bo.PersonalUserUpdateBO;
import com.ai.base.application.bo.PersonalUsernameUpdateBO;
import com.ai.base.application.bo.TenantBO;
import com.ai.base.application.bo.UserAccountBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.port.TenantAccountPort;
import com.ai.base.application.port.UserAccountPort;
import com.ai.base.application.port.UserProfileQueryPort;
import com.ai.base.application.port.UserTenantRelationPort;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.SmsCodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    private static final String USER_ID = "user-1";

    private UserAccountPort userAccountPort;
    private UserProfileQueryPort userProfileQueryPort;
    private UserTenantRelationPort userTenantRelationPort;
    private TenantAccountPort tenantAccountPort;
    private SmsCodeService smsCodeService;
    private LoginService loginService;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userAccountPort = mock(UserAccountPort.class);
        userProfileQueryPort = mock(UserProfileQueryPort.class);
        userTenantRelationPort = mock(UserTenantRelationPort.class);
        tenantAccountPort = mock(TenantAccountPort.class);
        smsCodeService = mock(SmsCodeService.class);
        loginService = mock(LoginService.class);
        userService = new UserServiceImpl(userAccountPort, userProfileQueryPort, userTenantRelationPort,
                tenantAccountPort, smsCodeService, loginService);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", "session-1", "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void updateCurrentProfileDoesNotChangeCredentials() {
        UserAccountBO user = user("old-account", "13800000000");
        when(userAccountPort.findUser(USER_ID)).thenReturn(Optional.of(user));
        when(userAccountPort.updateAccount(any(UserAccountBO.class))).thenReturn(true);
        PersonalUserUpdateBO update = new PersonalUserUpdateBO();
        update.setEmail("new@example.com");
        update.setName("new-name");

        userService.updateCurrentProfile(update);

        assertThat(user.getUsername()).isEqualTo("old-account");
        assertThat(user.getMobile()).isEqualTo("13800000000");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getName()).isEqualTo("new-name");
        verify(userAccountPort).upsertUserInfo(any());
        verifyNoInteractions(smsCodeService, loginService);
    }

    @Test
    void updateCurrentUsernameUpdatesIdentityAndLogsOutAllSessionsAfterCommit() {
        UserAccountBO user = user("old-account", "13800000000");
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.of(user));
        when(userAccountPort.findIdentity(IdentityTypeEnum.ACCOUNT.getValue(),
                IdentityProviderEnum.LOCAL.getValue(), "new-account")).thenReturn(Optional.empty());
        when(userAccountPort.updateAccount(any(UserAccountBO.class))).thenReturn(true);
        PersonalUsernameUpdateBO update = new PersonalUsernameUpdateBO();
        update.setUsername("new-account");
        TransactionSynchronizationManager.initSynchronization();

        userService.updateCurrentUsername(update);

        assertThat(user.getUsername()).isEqualTo("new-account");
        verify(userAccountPort).upsertIdentity(USER_ID, IdentityTypeEnum.ACCOUNT.getValue(),
                IdentityProviderEnum.LOCAL.getValue(), "new-account");
        verify(loginService, never()).logoutAll();
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(loginService).logoutAll();
    }

    @Test
    void updateCurrentUsernameMapsDuplicateKeyToCredentialAlreadyInUse() {
        UserAccountBO user = user("old-account", "13800000000");
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.of(user));
        when(userAccountPort.findIdentity(any(), any(), any())).thenReturn(Optional.empty());
        when(userAccountPort.updateAccount(any(UserAccountBO.class)))
                .thenThrow(new DuplicateKeyException("duplicate username"));
        PersonalUsernameUpdateBO update = new PersonalUsernameUpdateBO();
        update.setUsername("new-account");

        assertThatThrownBy(() -> userService.updateCurrentUsername(update))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.CREDENTIAL_ALREADY_IN_USE));
        verifyNoInteractions(loginService);
    }

    @Test
    void updateCurrentMobileVerifiesNewMobileAndLogsOutAllSessionsAfterCommit() {
        UserAccountBO user = user("account", "13800000000");
        when(userAccountPort.findActiveUser(USER_ID)).thenReturn(Optional.of(user));
        when(userAccountPort.findIdentity(IdentityTypeEnum.MOBILE.getValue(),
                IdentityProviderEnum.SMS.getValue(), "13900000000")).thenReturn(Optional.empty());
        when(userAccountPort.updateAccount(any(UserAccountBO.class))).thenReturn(true);
        PersonalMobileUpdateBO update = new PersonalMobileUpdateBO();
        update.setMobile("13900000000");
        update.setSmsCode("123456");
        TransactionSynchronizationManager.initSynchronization();

        userService.updateCurrentMobile(update);

        verify(smsCodeService).consumeLoginCode("13900000000", "123456");
        assertThat(user.getMobile()).isEqualTo("13900000000");
        verify(userAccountPort).upsertIdentity(USER_ID, IdentityTypeEnum.MOBILE.getValue(),
                IdentityProviderEnum.SMS.getValue(), "13900000000");
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(loginService).logoutAll();
    }

    @Test
    void updateCurrentPersonalTenantRenamesActivePersonalTenant() {
        when(userTenantRelationPort.findPersonalTenantId(USER_ID)).thenReturn(Optional.of("tenant-1"));
        when(tenantAccountPort.findPersonalTenant("tenant-1")).thenReturn(Optional.of(new TenantBO()));

        PersonalTenantUpdateBO update = new PersonalTenantUpdateBO();
        update.setTenantName("我的空间");

        userService.updateCurrentPersonalTenant(update);

        verify(tenantAccountPort).renameTenant("tenant-1", "我的空间");
    }

    private UserAccountBO user(String username, String mobile) {
        UserAccountBO user = new UserAccountBO();
        user.setUserId(USER_ID);
        user.setUsername(username);
        user.setMobile(mobile);
        user.setStatus(1);
        return user;
    }
}
