package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordLoginAuthenticatorImpl implements LoginAuthenticator {
    private final UserIdentityMapper userIdentityMapper;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordLoginAuthenticatorImpl(UserIdentityMapper userIdentityMapper, UserMapper userMapper) {
        this.userIdentityMapper = userIdentityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public LoginTypeEnum supports() {
        return LoginTypeEnum.ACCOUNT_PASSWORD;
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginCommand command) {
        List<ErrorCodeEnum> errorDetails = new ArrayList<>();
        if (!StringUtils.hasText(command.getAccount())) {
            errorDetails.add(ErrorCodeEnum.ACCOUNT_REQUIRED);
        }
        if (!StringUtils.hasText(command.getPassword())) {
            errorDetails.add(ErrorCodeEnum.PASSWORD_REQUIRED);
        }
        if (!errorDetails.isEmpty()) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, errorDetails);
        }
        UserIdentityEntity accountIdentity = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                .eq(UserIdentityEntity::getIdentityType, IdentityTypeEnum.ACCOUNT.getValue())
                .eq(UserIdentityEntity::getIdentityProvider, IdentityProviderEnum.LOCAL.getValue())
                .eq(UserIdentityEntity::getIdentityValue, command.getAccount()));
        if (accountIdentity == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, accountIdentity.getUserId())
                .eq(UserEntity::getStatus, 1));
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(accountIdentity.getUserId());
        identity.setIdentityType(IdentityTypeEnum.ACCOUNT.getValue());
        identity.setIdentityProvider(IdentityProviderEnum.LOCAL.getValue());
        identity.setIdentityValue(command.getAccount());
        identity.setMobile(user.getMobile());
        return identity;
    }
}

