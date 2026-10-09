package com.ai.base.infrastructure.persistence.dto;

import lombok.Getter;
import lombok.Setter;

/** 用户所属租户查询结果行。 */
@Getter
@Setter
public class UserTenantRow {
    /** 全局用户标识。 */
    private String userId;
    /** 全局租户标识。 */
    private String tenantId;
    /** 租户名称。 */
    private String tenantName;
    /** 租户类型。 */
    private Integer tenantType;
}

