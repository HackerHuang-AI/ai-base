package com.ai.base.application.enums;

import lombok.Getter;

/** 租户类型。 */
@Getter
public enum TenantTypeEnum {
    /** 用户创建的个人空间。 */
    PERSONAL(1, "个人空间"),
    /** 企业创建的协作租户。 */
    ENTERPRISE(2, "企业租户");

    private final Integer value;
    private final String description;

    TenantTypeEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public static String descriptionOf(Integer value) {
        for (TenantTypeEnum tenantType : values()) {
            if (tenantType.value.equals(value)) {
                return tenantType.description;
            }
        }
        return null;
    }
}

