package com.ai.base.application.service.impl;

import com.ai.base.application.bo.PersonalMobileUpdateBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.bo.PersonalUserUpdateBO;
import com.ai.base.application.bo.PersonalUsernameUpdateBO;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.entity.UserInfoEntity;
import com.ai.base.infrastructure.persistence.mapper.TenantMapper;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserInfoMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import com.ai.base.infrastructure.persistence.mapper.extension.UserExtensionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceImplTest {
    private static final String USER_ID = "user-1";

    private UserMapper userMapper;
    private UserInfoMapper userInfoMapper;
    private UserIdentityMapper userIdentityMapper;
    private SmsCodeService smsCodeService;
    private LoginService loginService;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userInfoMapper = mock(UserInfoMapper.class);
        userIdentityMapper = mock(UserIdentityMapper.class);
        smsCodeService = mock(SmsCodeService.class);
        loginService = mock(LoginService.class);
        userService = new UserServiceImpl(mock(UserExtensionMapper.class), mock(TenantUserExtensionMapper.class),
                userMapper, userInfoMapper, userIdentityMapper, mock(TenantMapper.class), smsCodeService, loginService);
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
        UserEntity user = user("old-account", "13800000000");
        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setUserId(USER_ID);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(userInfoMapper.selectOne(any())).thenReturn(userInfo);
        when(userInfoMapper.updateById(userInfo)).thenReturn(1);
        PersonalUserUpdateBO update = new PersonalUserUpdateBO();
        update.setEmail("new@example.com");
        update.setName("new-name");

        userService.updateCurrentProfile(update);

        assertThat(user.getUsername()).isEqualTo("old-account");
        assertThat(user.getMobile()).isEqualTo("13800000000");
        verifyNoInteractions(userIdentityMapper, smsCodeService, loginService);
    }

    @Test
    void updateCurrentUsernameUpdatesIdentityAndLogsOutAllSessionsAfterCommit() {
        UserEntity user = user("old-account", "13800000000");
        UserIdentityEntity identity = new UserIdentityEntity();
        identity.setUserId(USER_ID);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(userIdentityMapper.selectOne(any())).thenReturn(null, identity);
        when(userIdentityMapper.updateById(identity)).thenReturn(1);
        PersonalUsernameUpdateBO update = new PersonalUsernameUpdateBO();
        update.setUsername("new-account");
        TransactionSynchronizationManager.initSynchronization();

        userService.updateCurrentUsername(update);

        assertThat(user.getUsername()).isEqualTo("new-account");
        assertThat(identity.getIdentityValue()).isEqualTo("new-account");
        verify(loginService, never()).logoutAll();
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(loginService).logoutAll();
    }

    @Test
    void updateCurrentUsernameMapsDuplicateKeyToCredentialAlreadyInUse() {
        UserEntity user = user("old-account", "13800000000");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userIdentityMapper.selectOne(any())).thenReturn(null);
        when(userMapper.updateById(user)).thenThrow(new DuplicateKeyException("duplicate username"));
        PersonalUsernameUpdateBO update = new PersonalUsernameUpdateBO();
        update.setUsername("new-account");

        assertThatThrownBy(() -> userService.updateCurrentUsername(update))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnum.CREDENTIAL_ALREADY_IN_USE));
        verifyNoInteractions(loginService);
    }

    @Test
    void updateCurrentMobileVerifiesNewMobileAndLogsOutAllSessionsAfterCommit() {
        UserEntity user = user("account", "13800000000");
        UserIdentityEntity identity = new UserIdentityEntity();
        identity.setUserId(USER_ID);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(userIdentityMapper.selectOne(any())).thenReturn(null, identity);
        when(userIdentityMapper.updateById(identity)).thenReturn(1);
        PersonalMobileUpdateBO update = new PersonalMobileUpdateBO();
        update.setMobile("13900000000");
        update.setSmsCode("123456");
        TransactionSynchronizationManager.initSynchronization();

        userService.updateCurrentMobile(update);

        verify(smsCodeService).consumeLoginCode("13900000000", "123456");
        assertThat(user.getMobile()).isEqualTo("13900000000");
        assertThat(identity.getIdentityValue()).isEqualTo("13900000000");
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(loginService).logoutAll();
    }

    private UserEntity user(String username, String mobile) {
        UserEntity user = new UserEntity();
        user.setUserId(USER_ID);
        user.setUsername(username);
        user.setMobile(mobile);
        user.setStatus(1);
        return user;
    }
}

