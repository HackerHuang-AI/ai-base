package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 当前个人用户资料更新参数。 */
@Getter
@Setter
public class PersonalUserUpdateBO {
    /** 邮箱地址。 */
    private String email;
    /** 姓名。 */
    private String name;
    /** 头像地址。 */
    private String avatarUrl;
    /** 工号。 */
    private String jobNumber;
}

