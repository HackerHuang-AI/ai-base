package com.ai.base.application.model;

import java.time.LocalDateTime;

/**
 * 登录审计条目。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public record LoginAuditEntry(String userId, String tenantId, String identityType, String identityValueHash,
                              Integer loginResult, String loginIp, String userAgent, LocalDateTime loginAt) {
}
