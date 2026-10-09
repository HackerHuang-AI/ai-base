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
    /** 适用范围：1-租户，2-组织，3-两者。 */
    private Integer scopeType;
    /** 角色说明。 */
    private String description;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
}

