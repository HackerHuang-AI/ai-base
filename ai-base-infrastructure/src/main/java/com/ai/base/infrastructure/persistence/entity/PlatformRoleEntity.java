package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("platform_role")
public class PlatformRoleEntity extends BaseEntity {
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
}

