package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 管理端用户分页查询条件。 */
@Getter
@Setter
public class AdminUserQueryBO {
    /** 全局用户标识。 */
    private String userId;
    /** 手机号，模糊匹配。 */
    private String mobile;
    /** 邮箱，模糊匹配。 */
    private String email;
    /** 用户名，模糊匹配。 */
    private String username;
    /** 姓名，模糊匹配。 */
    private String name;
    /** 头像地址，模糊匹配。 */
    private String avatarUrl;
    /** 工号，模糊匹配。 */
    private String jobNumber;
    /** 用户状态：1-正常，2-禁用。 */
    private Integer status;
    /** 创建时间起始值，包含该时间。 */
    private LocalDateTime createdStartTime;
    /** 创建时间结束值，包含该时间。 */
    private LocalDateTime createdEndTime;
    /** 页码，从 1 开始。 */
    private long pageNo;
    /** 每页记录数。 */
    private long pageSize;
}

