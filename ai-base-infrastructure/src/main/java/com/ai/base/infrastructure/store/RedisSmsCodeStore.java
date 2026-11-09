package com.ai.base.infrastructure.store;

import com.ai.base.application.port.SmsCodeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 短信验证码存储端口实现：承载验证码相关的 Redis key 结构。
 *
 * <p>key 约定：ai-base:auth:sms-code:{mobileHash}、ai-base:auth:sms-limit:{mobileHash}。</p>
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class RedisSmsCodeStore implements SmsCodeStore {

    private static final String CODE_KEY_PREFIX = "ai-base:auth:sms-code:";
    private static final String LIMIT_KEY_PREFIX = "ai-base:auth:sms-limit:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisSmsCodeStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryAcquireSendLimit(String mobileHash, Duration ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(LIMIT_KEY_PREFIX + mobileHash, "1", ttl));
    }

    @Override
    public void saveCode(String mobileHash, String code, Duration ttl) {
        stringRedisTemplate.opsForValue().set(CODE_KEY_PREFIX + mobileHash, code, ttl);
    }

    @Override
    public void deleteCodeAndLimit(String mobileHash) {
        stringRedisTemplate.delete(CODE_KEY_PREFIX + mobileHash);
        stringRedisTemplate.delete(LIMIT_KEY_PREFIX + mobileHash);
    }

    @Override
    public String consumeCode(String mobileHash) {
        return stringRedisTemplate.opsForValue().getAndDelete(CODE_KEY_PREFIX + mobileHash);
    }
}
