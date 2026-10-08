package com.ai.base.application.utils;

import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;

/**
 * 当前请求用户上下文访问工具。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public final class CurrentUserUtil {
    private CurrentUserUtil() {
    }

    public static String requireUserId() {
        return RequestContextHolder.current()
                .map(RequestContext::userId)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("current user is unavailable"));
    }

    public static String requireSessionId() {
        return RequestContextHolder.current()
                .map(RequestContext::sessionId)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("current session is unavailable"));
    }

    public static String requireTenantId() {
        return RequestContextHolder.current()
                .map(RequestContext::tenantId)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("current tenant is unavailable"));
    }
}

