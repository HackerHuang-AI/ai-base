package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 当前用户手机号变更参数。 */
@Getter
@Setter
public class PersonalMobileUpdateBO {
    /** 新手机号。 */
    private String mobile;
    /** 新手机号收到的短信验证码。 */
    private String smsCode;
}

