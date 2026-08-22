package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("base_user")
public class UserEntity extends BaseEntity {
    private String ssoUserId;
    private String username;
    private String name;
    private Integer status;
}

