package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("role_permission")
public class RolePermissionEntity extends BaseEntity {
    /** 角色 ID。 */
    private Long roleId;
    /** 权限点 ID。 */
    private Long permissionId;
}

