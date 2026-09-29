package com.ai.base.application.service;

import com.ai.base.application.auth.AuthenticatedIdentity;
import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.enums.LoginTypeEnum;

/**
 * @Description: 按登录方式认证用户身份的接口。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.service
 * @ClassName: LoginAuthenticator
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public interface LoginAuthenticator {
    LoginTypeEnum supports();

    AuthenticatedIdentity authenticate(LoginCommand command);
}

