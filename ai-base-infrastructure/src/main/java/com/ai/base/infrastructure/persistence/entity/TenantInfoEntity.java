package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tenant_info")
public class TenantInfoEntity extends BaseEntity {
    private Long tenantId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String description;
}

