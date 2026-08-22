package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("organization_structure_node")
public class OrganizationStructureNodeEntity extends BaseEntity {
    private Long tenantId;
    private Long structureId;
    private Long organizationId;
    private Long parentNodeId;
    private Integer sortNo;
    private Long creatorUserId;
    private Long updaterUserId;
}

