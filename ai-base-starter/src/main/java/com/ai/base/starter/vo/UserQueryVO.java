package com.ai.base.starter.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用户详情与分页查询请求参数。 */
@Getter
@Setter
public class UserQueryVO {
    /** 全局用户 ID；查询用户详情时必填。 */
    private String userId;
    /** 手机号，分页查询时模糊匹配。 */
    private String mobile;
    /** 邮箱，分页查询时模糊匹配。 */
    private String email;
    /** 账号名，分页查询时模糊匹配。 */
    private String username;
    /** 用户姓名，分页查询时模糊匹配。 */
    private String name;
    /** 头像地址，分页查询时模糊匹配。 */
    private String avatarUrl;
    /** 工号，分页查询时模糊匹配。 */
    private String jobNumber;
    /** 用户状态：1-正常，2-禁用。 */
    private Integer status;
    /** 创建时间起始，包含该时间。 */
    private LocalDateTime createdStartTime;
    /** 创建时间结束，包含该时间。 */
    private LocalDateTime createdEndTime;
    /** 页码，从 1 开始，默认 1。 */
    @Min(value = 1, message = "1201102")
    private long pageNo = 1;
    /** 每页条数，默认 20，最大 100。 */
    @Min(value = 1, message = "1201103")
    @Max(value = 100, message = "1201103")
    private long pageSize = 20;
}

