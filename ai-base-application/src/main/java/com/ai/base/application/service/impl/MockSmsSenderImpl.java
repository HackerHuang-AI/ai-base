package com.ai.base.application.service.impl;

import com.ai.base.application.service.SmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "test"})
public class MockSmsSenderImpl implements SmsSender {
    @Override
    public void sendLoginCode(String mobile, String code) {
        log.info("本地短信验证码已发送, mobile={}", maskMobile(mobile));
    }

    private String maskMobile(String mobile) {
        return mobile.length() <= 4 ? "****" : mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}

