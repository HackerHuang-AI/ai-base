package com.ai.base.application.service.impl;

import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.SmsCodeService;
import com.ai.base.application.service.SmsSender;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class SmsCodeServiceImpl implements SmsCodeService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_LIMIT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate stringRedisTemplate;
    private final SmsSender smsSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public SmsCodeServiceImpl(StringRedisTemplate stringRedisTemplate, SmsSender smsSender) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.smsSender = smsSender;
    }

    @Override
    public void sendLoginCode(String mobile) {
        validateMobile(mobile);
        String mobileHash = sha256(mobile);
        String codeKey = "ai-admin:ai-base:auth:sms-code:" + mobileHash;
        String limitKey = "ai-admin:ai-base:auth:sms-limit:" + mobileHash;
        if (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", SEND_LIMIT_TTL))) {
            throw new BizException(ErrorCodeEnum.SMS_CODE_SEND_TOO_FREQUENT);
        }
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL);
        try {
            smsSender.sendLoginCode(mobile, code);
        } catch (RuntimeException exception) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(limitKey);
            throw exception;
        }
    }

    @Override
    public void consumeLoginCode(String mobile, String smsCode) {
        validateMobile(mobile);
        String code = stringRedisTemplate.opsForValue().getAndDelete("ai-admin:ai-base:auth:sms-code:" + sha256(mobile));
        if (!StringUtils.hasText(smsCode) || !smsCode.equals(code)) {
            throw new BizException(ErrorCodeEnum.SMS_CODE_INVALID);
        }
    }

    private void validateMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            throw new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.MOBILE_REQUIRED);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}

