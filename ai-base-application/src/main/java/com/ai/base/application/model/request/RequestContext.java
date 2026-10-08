package com.ai.base.application.model.request;

/**
 * 请求上下文信息。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public record RequestContext(
        /** 调用链追踪标识。 */
        String traceId,
        /** W3C Trace Context 追踪信息。 */
        String traceparent,
        /** 当前用户标识。 */
        String userId,
        /** 当前租户标识。 */
        String tenantId,
        /** 当前会话标识。 */
        String sessionId,
        /** 调用链信息。 */
        String callChain
) {
}

