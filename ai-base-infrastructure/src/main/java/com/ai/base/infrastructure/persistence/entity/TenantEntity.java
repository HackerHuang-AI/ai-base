package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("tenant")
public class TenantEntity extends BaseEntity {
    private String tenantCode;
    private String tenantName;
    private Integer tenantType;
    private String creditCode;
    private Long ownerUserId;
    private Integer status;
}

