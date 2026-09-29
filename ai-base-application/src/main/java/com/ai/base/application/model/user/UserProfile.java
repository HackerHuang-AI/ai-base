package com.ai.base.application.model.user;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 用户资料。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.model.user
 * @ClassName: UserProfile
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
public class UserProfile {
    /** 用户标识。 */
    private String userId;
    /** 手机号。 */
    private String mobile;
    /** 邮箱地址。 */
    private String email;
    /** 用户名。 */
    private String username;
    /** 姓名。 */
    private String name;
    /** 头像地址。 */
    private String avatarUrl;
    /** 工号。 */
    private String jobNumber;
}

