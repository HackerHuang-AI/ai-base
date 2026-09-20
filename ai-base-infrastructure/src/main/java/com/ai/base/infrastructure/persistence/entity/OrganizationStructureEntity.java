package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("organization_structure")
public class OrganizationStructureEntity extends BaseEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 组织架构编码。 */
    private String structureCode;
    /** 组织架构名称。 */
    private String structureName;
    /** 是否主组织架构：1-是，0-否。 */
    private Integer isPrimary;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

