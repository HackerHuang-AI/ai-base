package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 用户所属租户信息。 */
@Getter
@Setter
public class UserTenantBO {
    /** 全局租户标识。 */
    private String tenantId;
    /** 租户名称。 */
    private String tenantName;
    /** 租户类型：1-个人空间，2-企业租户。 */
    private Integer tenantType;
    /** 租户类型描述。 */
    private String tenantTypeDescription;
}

