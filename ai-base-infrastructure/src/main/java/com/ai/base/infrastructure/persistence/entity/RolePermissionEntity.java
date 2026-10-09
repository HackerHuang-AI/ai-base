package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色权限关联实体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
@TableName("role_permission")
public class RolePermissionEntity extends VersionedEntity {
    /** 角色 ID。 */
    private Long roleId;
    /** 权限点 ID。 */
    private Long permissionId;
}

