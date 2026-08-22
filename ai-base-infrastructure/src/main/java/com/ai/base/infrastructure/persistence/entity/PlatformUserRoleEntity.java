package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("platform_user_role")
public class PlatformUserRoleEntity extends BaseEntity {
    private Long userId;
    private Long platformRoleId;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
}

