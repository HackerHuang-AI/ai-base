package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_role")
public class UserRoleEntity extends BaseEntity {
    private Long tenantUserId;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private Long roleId;
}

