package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("role")
public class RoleEntity extends BaseEntity {
    private Long tenantId;
    private String roleCode;
    private String roleName;
    private Integer scopeType;
    private String description;
    private Integer status;
}

