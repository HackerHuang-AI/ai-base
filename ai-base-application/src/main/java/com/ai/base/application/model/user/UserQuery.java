package com.ai.base.application.model.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用户详情与分页查询的应用层查询条件。 */
@Getter
@Setter
public class UserQuery {
    /** 全局用户 ID。 */
    private String userId;
    /** 手机号，模糊匹配。 */
    private String mobile;
    /** 邮箱，模糊匹配。 */
    private String email;
    /** 账号名，模糊匹配。 */
    private String username;
    /** 用户姓名，模糊匹配。 */
    private String name;
    /** 头像地址，模糊匹配。 */
    private String avatarUrl;
    /** 工号，模糊匹配。 */
    private String jobNumber;
    /** 用户状态：1-正常，2-禁用。 */
    private Integer status;
    /** 创建时间起始，包含该时间。 */
    private LocalDateTime createdStartTime;
    /** 创建时间结束，包含该时间。 */
    private LocalDateTime createdEndTime;
    /** 页码，从 1 开始。 */
    private long pageNo = 1;
    /** 每页条数。 */
    private long pageSize = 20;
}

