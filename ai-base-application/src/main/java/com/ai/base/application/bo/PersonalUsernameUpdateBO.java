package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 当前用户账号名变更参数。 */
@Getter
@Setter
public class PersonalUsernameUpdateBO {
    /** 新账号名。 */
    private String username;
}

