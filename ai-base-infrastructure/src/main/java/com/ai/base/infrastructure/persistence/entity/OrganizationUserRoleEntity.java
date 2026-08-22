package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("organization_user_role")
public class OrganizationUserRoleEntity extends BaseEntity {
    private Long organizationUserId;
    private Long roleId;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
}

