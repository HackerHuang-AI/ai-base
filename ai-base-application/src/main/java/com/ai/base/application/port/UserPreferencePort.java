package com.ai.base.application.port;

import com.ai.base.application.bo.UserTenantPreferenceBO;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户租户偏好存取端口。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface UserPreferencePort {

    /** 按用户查询偏好。 */
    Optional<UserTenantPreferenceBO> findByUserId(String userId);

    /**
     * 插入偏好：不存在时写入，唯一键冲突返回 false。
     *
     * @param userId           用户标识
     * @param defaultTenantId  默认租户标识，可为 null
     * @param lastUsedTenantId 最近使用租户标识，可为 null
     * @param lastUsedAt       最近使用时间，可为 null
     */
    boolean insertPreference(String userId, String defaultTenantId, String lastUsedTenantId, LocalDateTime lastUsedAt);

    /** 更新默认租户，返回 false 表示记录不存在或版本冲突。 */
    boolean updateDefaultTenantId(String userId, String tenantId);

    /** 更新最近使用租户，返回 false 表示记录不存在或版本冲突。 */
    boolean updateLastUsed(String userId, String tenantId, LocalDateTime usedAt);
}
