package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("tenant_user")
public class TenantUserEntity extends BaseEntity {
    private Long tenantId;
    private Long userId;
    private Integer status;
    private LocalDateTime joinedAt;
    private Long creatorUserId;
    private Long updaterUserId;
}

