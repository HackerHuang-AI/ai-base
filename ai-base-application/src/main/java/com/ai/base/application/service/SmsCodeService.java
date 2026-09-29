package com.ai.base.application.service;

/**
 * @Description: 短信登录验证码服务接口。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.service
 * @ClassName: SmsCodeService
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public interface SmsCodeService {
    void sendLoginCode(String mobile);

    void consumeLoginCode(String mobile, String smsCode);
}

