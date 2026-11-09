package com.ai.base.application.model;

import java.time.Duration;

/**
 * 会话策略：TTL 与滑动续期窗口。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public record SessionPolicy(Duration ttl, Duration renewWindow) {
}
