package com.ai.base.application.model.request;

import java.util.Optional;

/**
 * @Description: 请求上下文线程持有器。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.model.request
 * @ClassName: RequestContextHolder
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    public static Optional<RequestContext> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static void set(RequestContext context) {
        HOLDER.set(context);
    }

    public static void clear() {
        HOLDER.remove();
    }
}

