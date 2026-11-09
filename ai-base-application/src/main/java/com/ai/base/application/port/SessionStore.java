package com.ai.base.application.port;

import com.ai.base.application.model.SessionRecord;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 会话存储端口：屏蔽 Redis key 结构、Lua 脚本与序列化细节。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface SessionStore {

    /**
     * 原子占用设备的会话槽位：同设备旧会话被替换，超设备数上限时拒绝。
     *
     * @return 被替换的旧会话标识；deviceLimitExceeded 为 true 时 oldSessionId 无意义
     */
    DeviceSlot replaceDeviceSlot(String userId, String deviceId, String sessionId, Duration ttl, int maxDevices);

    /** 会话主记录写入失败后回滚设备槽位。 */
    void rollbackDeviceSlot(String userId, String deviceId, String sessionId, String oldSessionId);

    /** 写入会话主记录，TTL 取自记录内的 ttlMillis。 */
    void saveSession(SessionRecord session, String tokenHash);

    /** 按会话令牌哈希读取会话记录。 */
    Optional<SessionRecord> findSession(String tokenHash);

    /** 按会话标识读取令牌哈希。 */
    Optional<String> findTokenHash(String sessionId);

    /** 删除会话主记录与会话标识索引。 */
    void deleteSession(String tokenHash, String sessionId);

    /** 列出用户全部在途会话标识。 */
    List<String> listSessionIds(String userId);

    /** 移除用户指定设备的会话槽位。 */
    void removeDevice(String userId, String deviceId);

    /**
     * 原子切换会话租户：校验会话归属与当前租户后改写会话中的租户标识。
     */
    TenantSwitchResult switchTenant(String tokenHash, String userId, String sessionId,
                                    String currentTenantId, String newTenantId);

    /** 设备槽位占用结果。 */
    record DeviceSlot(String oldSessionId, boolean deviceLimitExceeded) {
    }

    /** 租户切换结果。 */
    enum TenantSwitchResult {
        /** 切换成功。 */
        OK,
        /** 会话不存在或已过期。 */
        SESSION_INVALID,
        /** 会话当前租户与请求声明的租户不一致。 */
        TENANT_MISMATCH
    }
}
