package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 发送短信验证码请求参数。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.starter.vo
 * @ClassName: SendSmsCodeRequestVO
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
public class SendSmsCodeRequestVO {
    /** 接收短信验证码的手机号。 */
    @NotBlank(message = "1001101")
    private String mobile;
}

