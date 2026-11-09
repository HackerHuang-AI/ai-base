package com.ai.base.application.bo;

import lombok.Data;

/**
 * 用户身份凭证（user_identity 表语义字段）。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Data
public class UserIdentityBO {
    private String userId;
    private String identityType;
    private String identityProvider;
    private String identityValue;
}
