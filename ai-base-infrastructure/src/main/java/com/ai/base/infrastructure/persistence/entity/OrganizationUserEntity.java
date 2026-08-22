package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("organization_user")
public class OrganizationUserEntity extends BaseEntity {
    private Long tenantId;
    private Long organizationId;
    private Long tenantUserId;
    private Long positionId;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private Integer status;
    private Long creatorUserId;
    private Long updaterUserId;
}

