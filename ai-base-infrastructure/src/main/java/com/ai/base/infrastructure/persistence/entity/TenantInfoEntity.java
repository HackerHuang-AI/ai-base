package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tenant_info")
public class TenantInfoEntity extends BaseEntity {
    /** 全局租户 ID。 */
    private String tenantId;
    /** 联系人姓名。 */
    private String contactName;
    /** 联系人手机号。 */
    private String contactPhone;
    /** 联系人邮箱。 */
    private String contactEmail;
    /** 企业地址。 */
    private String address;
    /** 租户简介。 */
    private String description;
}

