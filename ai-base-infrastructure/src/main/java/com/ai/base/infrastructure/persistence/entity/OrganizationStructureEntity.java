package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("organization_structure")
public class OrganizationStructureEntity extends BaseEntity {
    private Long tenantId;
    private String structureCode;
    private String structureName;
    private Integer isPrimary;
    private Integer status;
    private Long creatorUserId;
    private Long updaterUserId;
}

