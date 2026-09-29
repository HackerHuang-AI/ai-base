package com.ai.base.application.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 用户登录结果。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.auth
 * @ClassName: LoginResult
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
public class LoginResult {
    /** 会话访问令牌。 */
    private String sessionToken;
    /** 会话标识。 */
    private String sessionId;
    /** 当前用户标识。 */
    private String userId;
    /** 当前租户标识。 */
    private String tenantId;
}

