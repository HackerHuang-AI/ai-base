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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordLoginAuthenticatorImpl implements LoginAuthenticator {
    private final UserAccountPort userAccountPort;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordLoginAuthenticatorImpl(UserAccountPort userAccountPort) {
        this.userAccountPort = userAccountPort;
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
        UserAccountBO accountIdentity = userAccountPort.findIdentity(
                        IdentityTypeEnum.ACCOUNT.getValue(), IdentityProviderEnum.LOCAL.getValue(), command.getAccount())
                .flatMap(identity -> userAccountPort.findActiveUser(identity.getUserId()))
                .orElse(null);
        if (accountIdentity == null || accountIdentity.getPasswordHash() == null
                || !passwordEncoder.matches(command.getPassword(), accountIdentity.getPasswordHash())) {
            throw new BizException(ErrorCodeEnum.LOGIN_FAILED);
        }
        AuthenticatedIdentity identity = new AuthenticatedIdentity();
        identity.setUserId(accountIdentity.getUserId());
        identity.setIdentityType(IdentityTypeEnum.ACCOUNT.getValue());
        identity.setIdentityProvider(IdentityProviderEnum.LOCAL.getValue());
        identity.setIdentityValue(command.getAccount());
        identity.setMobile(accountIdentity.getMobile());
        return identity;
    }
}
