package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.bo.UserAccountBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.port.UserAccountPort;
import com.ai.base.application.service.LoginAuthenticator;
import com.ai.base.application.service.SmsCodeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class SmsLoginAuthenticatorImpl implements LoginAuthenticator {
    private final SmsCodeService smsCodeService;
    private final UserAccountPort userAccountPort;

    public SmsLoginAuthenticatorImpl(SmsCodeService smsCodeService, UserAccountPort userAccountPort) {
        this.smsCodeService = smsCodeService;
        this.userAccountPort = userAccountPort;
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
        UserAccountBO user = userAccountPort.findIdentity(
                        IdentityTypeEnum.MOBILE.getValue(), IdentityProviderEnum.SMS.getValue(), command.getMobile())
                .flatMap(identity -> userAccountPort.findActiveUser(identity.getUserId()))
                .orElse(null);
        if (user == null) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(user.getUserId());
        identity.setIdentityType(IdentityTypeEnum.MOBILE.getValue());
        identity.setIdentityProvider(IdentityProviderEnum.SMS.getValue());
        identity.setIdentityValue(command.getMobile());
        identity.setMobile(command.getMobile());
        return identity;
    }
}
