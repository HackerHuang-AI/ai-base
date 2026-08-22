package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("role_permission")
public class RolePermissionEntity extends BaseEntity {
    private Long roleId;
    private Long permissionId;
}

