package com.ai.base.starter.common;

/**
 * 接口响应中的错误明细。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public record ErrorItem(
        /** 错误码。 */
        String errorCode,
        /** 错误消息。 */
        String message
) {
}

