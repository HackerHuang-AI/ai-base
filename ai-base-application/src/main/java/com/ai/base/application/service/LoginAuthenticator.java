package com.ai.base.application.service;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.enums.LoginTypeEnum;

/**
 * 按登录方式认证用户身份的接口。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public interface LoginAuthenticator {
    LoginTypeEnum supports();

    AuthenticatedIdentity authenticate(LoginCommand command);
}

