package com.ai.base.application.enums;

import lombok.Getter;

@Getter
public enum IdentityProviderEnum {
    /** 本地账号密码认证服务。 */
    LOCAL("LOCAL"),
    /** 短信验证码认证服务。 */
    SMS("SMS");

    private final String value;

    IdentityProviderEnum(String value) {
        this.value = value;
    }
}

