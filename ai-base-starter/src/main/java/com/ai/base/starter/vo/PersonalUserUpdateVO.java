package com.ai.base.starter.vo;

import lombok.Getter;
import lombok.Setter;

/** 修改当前个人用户资料的请求参数。 */
@Getter
@Setter
public class PersonalUserUpdateVO {
    /** 邮箱地址。 */
    private String email;
    /** 姓名。 */
    private String name;
    /** 头像地址。 */
    private String avatarUrl;
    /** 工号。 */
    private String jobNumber;
}

