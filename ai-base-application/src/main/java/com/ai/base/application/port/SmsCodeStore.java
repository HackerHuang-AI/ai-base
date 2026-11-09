package com.ai.base.application.port;

import java.time.Duration;

/**
 * 短信验证码存储端口：屏蔽 Redis key 结构与 TTL 细节，键由手机号哈希派生。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface SmsCodeStore {

    /** 尝试占用发送频控槽位，占用失败表示发送过于频繁。 */
    boolean tryAcquireSendLimit(String mobileHash, Duration ttl);

    /** 保存验证码。 */
    void saveCode(String mobileHash, String code, Duration ttl);

    /** 发送失败时清理验证码与频控槽位。 */
    void deleteCodeAndLimit(String mobileHash);

    /** 原子读取并删除验证码，不存在时返回 null。 */
    String consumeCode(String mobileHash);
}
