package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 租户角色实体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
@TableName("role")
public class RoleEntity extends VersionedEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 角色编码。 */
    private String roleCode;
    /** 角色名称。 */
    private String roleName;
    /** 角色说明。 */
    private String description;
    /** 租户是否允许删除：1-允许，0-不允许。 */
    private Integer deletable;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

