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

    /** 认证成功返回账号可用且包含 userId 的身份信息，认证失败、身份未绑定或账号不可用时抛出异常。 */
    AuthenticatedIdentity authenticate(LoginCommand command);
}

