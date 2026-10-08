package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 发送短信验证码请求参数。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class SendSmsCodeRequestVO {
    /** 接收短信验证码的手机号。 */
    @NotBlank(message = "1001101")
    private String mobile;
}

