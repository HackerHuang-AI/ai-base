package com.ai.base.application.model;

import java.time.LocalDateTime;

/**
 * 会话记录，承载会话在存储层中的完整语义字段。
 *
 * @param sessionId         会话唯一标识
 * @param userId            用户标识
 * @param tenantId          当前租户标识
 * @param deviceId          登录设备标识
 * @param loginIp           登录 IP
 * @param userAgent         登录 UA
 * @param loginAt           登录时间
 * @param ttlMillis         会话 TTL（毫秒）
 * @param renewWindowMillis 滑动续期窗口（毫秒）
 * @author HUANGcong
 * @since 2026-11-07
 */
public record SessionRecord(String sessionId, String userId, String tenantId, String deviceId,
                            String loginIp, String userAgent, LocalDateTime loginAt,
                            long ttlMillis, long renewWindowMillis) {
}
