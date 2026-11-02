package com.ai.base.application.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户登录结果。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class LoginResult {
    /** 会话访问令牌。 */
    private String sessionToken;
    /** 当前租户标识。 */
    private String tenantId;
}

