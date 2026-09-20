package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("organization_structure_node")
public class OrganizationStructureNodeEntity extends BaseEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 组织架构 ID。 */
    private Long structureId;
    /** 组织信息 ID。 */
    private Long organizationId;
    /** 父架构节点 ID，NULL 表示唯一根节点。 */
    private Long parentNodeId;
    /** 同级节点排序值，升序排列。 */
    private Integer sortNo;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

