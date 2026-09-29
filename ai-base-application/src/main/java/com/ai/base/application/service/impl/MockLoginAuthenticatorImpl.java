package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.IdentityProviderEnum;
import com.ai.base.application.enums.IdentityTypeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.service.LoginAuthenticator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MockLoginAuthenticatorImpl implements LoginAuthenticator {
    @Override
    public LoginTypeEnum supports() {
        return LoginTypeEnum.MOCK;
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginCommand command) {
        if (!StringUtils.hasText(command.getMobile())) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.MOBILE_REQUIRED);
        }
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setIdentityType(IdentityTypeEnum.MOBILE.getValue());
        identity.setIdentityProvider(IdentityProviderEnum.MOCK.getValue());
        identity.setIdentityValue(command.getMobile());
        identity.setMobile(command.getMobile());
        return identity;
    }
}

