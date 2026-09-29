package com.ai.base.application.enums;

import lombok.Getter;

@Getter
public enum IdentityTypeEnum {
    /** 本地账号标识，例如管理员账号。 */
    ACCOUNT("ACCOUNT"),
    /** 手机号标识。 */
    MOBILE("MOBILE");

    private final String value;

    IdentityTypeEnum(String value) {
        this.value = value;
    }
}

