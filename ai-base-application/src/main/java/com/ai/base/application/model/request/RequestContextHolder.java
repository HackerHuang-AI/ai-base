package com.ai.base.application.model.request;

import java.util.Optional;

/**
 * 请求上下文线程持有器。
 *
 * @author HUANGcong
 * @since 2026-09-24
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

