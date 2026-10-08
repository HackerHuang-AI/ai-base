package com.ai.base.application.service;

/**
 * 短信登录验证码服务接口。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public interface SmsCodeService {
    void sendLoginCode(String mobile);

    void consumeLoginCode(String mobile, String smsCode);
}

