package com.ai.base.application.service;

import com.ai.base.application.auth.LoginCommand;
import com.ai.base.application.auth.LoginResult;
import com.ai.base.application.auth.SessionInfo;

import java.util.List;

/**
 * 用户登录与会话管理服务接口。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public interface LoginService {
    LoginResult login(LoginCommand command);

    void logout();

    void logout(String sessionId);

    void logoutAll();

    List<SessionInfo> listSessions();
}

