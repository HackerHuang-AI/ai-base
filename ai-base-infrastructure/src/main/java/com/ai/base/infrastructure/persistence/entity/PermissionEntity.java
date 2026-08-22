package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("permission")
public class PermissionEntity extends BaseEntity {
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private String action;
    private String description;
    private Integer status;
}

