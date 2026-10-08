package com.ai.base.application.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * 认证后的用户身份信息。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class AuthenticatedIdentity {
    /** 已绑定用户标识，未绑定时为空。 */
    private String userId;
    /** 身份标识类型。 */
    private String identityType;
    /** 身份提供方。 */
    private String identityProvider;
    /** 身份标识值。 */
    private String identityValue;
    /** 用户手机号。 */
    private String mobile;
}

