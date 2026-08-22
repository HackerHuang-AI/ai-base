package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("position")
public class PositionEntity extends BaseEntity {
    private Long tenantId;
    private String positionCode;
    private String positionName;
    private String description;
    private Integer status;
    private Long creatorUserId;
    private Long updaterUserId;
}

