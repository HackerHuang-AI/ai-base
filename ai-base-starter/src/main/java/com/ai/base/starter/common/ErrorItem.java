package com.ai.base.starter.common;

/**
 * @Description: 接口响应中的错误明细。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.starter.common
 * @ClassName: ErrorItem
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public record ErrorItem(
        /** 错误码。 */
        String errorCode,
        /** 错误消息。 */
        String message
) {
}

