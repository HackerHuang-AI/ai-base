package com.ai.base.infrastructure.persistence.query;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户分页查询持久化条件。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class UserQuery {
    /** 全局用户 ID。 */
    private String userId;
    /** 手机号。 */
    private String mobile;
    /** 邮箱。 */
    private String email;
    /** 账号名。 */
    private String username;
    /** 用户姓名。 */
    private String name;
    /** 头像地址。 */
    private String avatarUrl;
    /** 工号。 */
    private String jobNumber;
    /** 用户状态。 */
    private Integer status;
    /** 创建时间起始值。 */
    private LocalDateTime createdStartTime;
    /** 创建时间结束值。 */
    private LocalDateTime createdEndTime;
    /** 页码。 */
    private long pageNo;
    /** 每页记录数。 */
    private long pageSize;
    /** LIMIT 的起始位置。 */
    private long offset;
}

