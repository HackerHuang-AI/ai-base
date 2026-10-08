package com.ai.base.infrastructure.persistence.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户列表查询结果行。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class UserProfileRow {
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
}

