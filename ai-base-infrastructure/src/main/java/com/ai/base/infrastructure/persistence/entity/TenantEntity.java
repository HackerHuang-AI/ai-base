package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tenant")
public class TenantEntity extends BaseEntity {
    /** 租户编码。 */
    private String tenantCode;
    /** 租户名称。 */
    private String tenantName;
    /** 类型：1-个人空间，2-企业租户。 */
    private Integer tenantType;
    /** 统一社会信用代码，个人空间为空。 */
    private String creditCode;
    /** 状态：1-正常，2-停用。 */
    private Integer status;
}

