package com.ai.base.starter.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminUserPageVO extends BasePageInVO {

    private String userId;

    private String mobile;

    private String email;

    private String username;

    private String name;

    private String avatarUrl;

    private String jobNumber;

    private Integer status;

    private LocalDateTime createdStartTime;

    private LocalDateTime createdEndTime;

}

