package com.ai.base.application.utils;

import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;

/**
 * @Description: 当前请求用户上下文访问工具。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.utils
 * @ClassName: CurrentUserUtil
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
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

