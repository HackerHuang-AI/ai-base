package com.ai.base.application.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 用户登录认证请求参数。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.auth
 * @ClassName: LoginCommand
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
public class LoginCommand {
    /** 登录方式。 */
    private String loginType;
    /** 登录账号。 */
    private String account;
    /** 登录密码。 */
    private String password;
    /** 手机号。 */
    private String mobile;
    /** 短信验证码。 */
    private String smsCode;
    /** App 扫码确认登录标识。 */
    private String qrLoginId;
    /** 客户端设备标识。 */
    private String deviceId;
    /** 客户端 IP 地址。 */
    private String loginIp;
    /** 客户端 User-Agent。 */
    private String userAgent;
}

