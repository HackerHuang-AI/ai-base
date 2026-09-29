package com.ai.base.application.model.request;

/**
 * @Description: 请求上下文信息。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.model.request
 * @ClassName: RequestContext
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
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

