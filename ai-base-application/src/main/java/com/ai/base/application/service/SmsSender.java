package com.ai.base.application.service;

/**
 * 短信登录验证码发送接口。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public interface SmsSender {
    void sendLoginCode(String mobile, String code);
}

