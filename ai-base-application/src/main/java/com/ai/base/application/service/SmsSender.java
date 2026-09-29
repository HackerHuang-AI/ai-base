package com.ai.base.application.service;

/**
 * @Description: 短信登录验证码发送接口。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.service
 * @ClassName: SmsSender
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public interface SmsSender {
    void sendLoginCode(String mobile, String code);
}

