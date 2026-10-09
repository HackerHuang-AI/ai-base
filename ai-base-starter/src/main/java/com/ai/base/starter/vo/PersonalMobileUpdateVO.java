package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 修改当前用户手机号的请求参数。 */
@Getter
@Setter
public class PersonalMobileUpdateVO {
    /** 新手机号。 */
    @NotBlank(message = "1001101")
    private String mobile;
    /** 新手机号收到的短信验证码。 */
    @NotBlank(message = "1101105")
    private String smsCode;
}

