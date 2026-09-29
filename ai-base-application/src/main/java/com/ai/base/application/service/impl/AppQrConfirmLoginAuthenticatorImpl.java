package com.ai.base.application.service.impl;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.LoginTypeEnum;
import com.ai.base.application.service.LoginAuthenticator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AppQrConfirmLoginAuthenticatorImpl implements LoginAuthenticator {
    @Override
    public LoginTypeEnum supports() {
        return LoginTypeEnum.APP_QR_CONFIRM;
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginCommand command) {
        if (!StringUtils.hasText(command.getQrLoginId())) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.QR_LOGIN_ID_REQUIRED);
        }
        throw new BizException(ErrorCodeEnum.LOGIN_TYPE_NOT_AVAILABLE);
    }
}

