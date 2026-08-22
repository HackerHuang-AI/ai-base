package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_info")
public class UserInfoEntity extends BaseEntity {
    private Long userId;
    private String mobile;
    private String email;
    private String avatarUrl;
    private String jobNumber;
}

