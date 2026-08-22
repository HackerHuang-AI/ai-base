package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("platform_role_permission")
public class PlatformRolePermissionEntity extends BaseEntity {
    private Long platformRoleId;
    private Long permissionId;
}

