package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestVO {
    /** 登录方式。 */
    @NotBlank(message = "1101101")
    private String loginType;
    /** 登录账号。 */
    private String account;
    /** 登录密码。 */
    private String password;
    /** 手机号。 */
    private String mobile;
    /** 短信验证码。 */
    private String smsCode;
    /** 二维码登录标识。 */
    private String qrLoginId;
    /** 客户端安装实例标识。 */
    @NotBlank(message = "1101107")
    @Pattern(regexp = "^(\\s*|[A-Za-z0-9_-]{16,128})$", message = "1101108")
    private String deviceId;
}

