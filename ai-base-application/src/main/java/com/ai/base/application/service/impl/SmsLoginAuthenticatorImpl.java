package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserIdentityEntity;
import com.ai.base.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ai.base.infrastructure.persistence.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class SmsLoginAuthenticatorImpl implements LoginAuthenticator {
    private final SmsCodeService smsCodeService;
    private final UserIdentityMapper userIdentityMapper;
    private final UserMapper userMapper;

    public SmsLoginAuthenticatorImpl(SmsCodeService smsCodeService, UserIdentityMapper userIdentityMapper,
                                     UserMapper userMapper) {
        this.smsCodeService = smsCodeService;
        this.userIdentityMapper = userIdentityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public LoginTypeEnum supports() {
        return LoginTypeEnum.MOBILE_SMS;
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginCommand command) {
        List<ErrorCodeEnum> errorDetails = new ArrayList<>();
        if (!StringUtils.hasText(command.getMobile())) {
            errorDetails.add(ErrorCodeEnum.MOBILE_REQUIRED);
        }
        if (!StringUtils.hasText(command.getSmsCode())) {
            errorDetails.add(ErrorCodeEnum.SMS_CODE_REQUIRED);
        }
        if (!errorDetails.isEmpty()) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, errorDetails);
        }
        smsCodeService.consumeLoginCode(command.getMobile(), command.getSmsCode());
        UserIdentityEntity mobileIdentity = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityEntity>()
                .eq(UserIdentityEntity::getIdentityType, IdentityTypeEnum.MOBILE.getValue())
                .eq(UserIdentityEntity::getIdentityProvider, IdentityProviderEnum.SMS.getValue())
                .eq(UserIdentityEntity::getIdentityValue, command.getMobile()));
        if (mobileIdentity == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId, mobileIdentity.getUserId())
                .eq(UserEntity::getStatus, 1));
        if (user == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(mobileIdentity.getUserId());
        identity.setIdentityType(IdentityTypeEnum.MOBILE.getValue());
        identity.setIdentityProvider(IdentityProviderEnum.SMS.getValue());
        identity.setIdentityValue(command.getMobile());
        identity.setMobile(command.getMobile());
        return identity;
    }
}

