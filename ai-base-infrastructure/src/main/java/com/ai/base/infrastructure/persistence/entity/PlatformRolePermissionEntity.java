package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台角色权限关联实体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
@TableName("platform_role_permission")
public class PlatformRolePermissionEntity extends VersionedEntity {
    /** 平台角色 ID。 */
    private Long platformRoleId;
    /** 权限点 ID。 */
    private Long permissionId;
}

