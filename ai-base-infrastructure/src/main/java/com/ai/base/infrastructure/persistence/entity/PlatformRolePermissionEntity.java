package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("platform_role_permission")
public class PlatformRolePermissionEntity extends BaseEntity {
    /** 平台角色 ID。 */
    private Long platformRoleId;
    /** 权限点 ID。 */
    private Long permissionId;
}

