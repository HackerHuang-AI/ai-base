package com.ai.base.application.service;

import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.LoginResult;
import com.ai.base.application.auth.SessionInfo;

import java.util.List;

/**
 * @Description: 用户登录与会话管理服务接口。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.service
 * @ClassName: LoginService
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public interface LoginService {
    LoginResult login(LoginCommand command);

    void logout();

    void logout(String sessionId);

    void logoutAll();

    List<SessionInfo> listSessions();
}

