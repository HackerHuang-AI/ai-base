package com.ai.base.infrastructure.persistence.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 用户列表查询结果行。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.dto
 * @ClassName: UserProfileRow
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
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

