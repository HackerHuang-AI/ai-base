package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("organization_info")
public class OrganizationInfoEntity extends BaseEntity {
    private Long tenantId;
    private String orgCode;
    private String orgName;
    private Integer orgType;
    private Integer status;
    private Long creatorUserId;
    private Long updaterUserId;
}

