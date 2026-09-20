package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("organization_info")
public class OrganizationInfoEntity extends BaseEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 组织编码。 */
    private String orgCode;
    /** 组织名称。 */
    private String orgName;
    /** 组织类型：1-部门，2-项目组，3-虚拟组织。 */
    private Integer orgType;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

