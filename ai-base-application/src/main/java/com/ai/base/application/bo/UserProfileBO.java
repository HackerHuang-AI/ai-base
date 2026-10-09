package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 用户基础资料。 */
@Getter
@Setter
public class UserProfileBO {
    /** 全局用户标识。 */
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

