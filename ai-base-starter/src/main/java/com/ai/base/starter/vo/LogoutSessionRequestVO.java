package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 指定会话注销请求参数。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class LogoutSessionRequestVO {
    /** 待注销的会话标识。 */
    @NotBlank(message = "1001003")
    private String sessionId;
}

