package com.ai.base.application.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户登录会话信息。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class SessionInfo {
    /** 会话标识。 */
    private String sessionId;
    /** 客户端设备标识。 */
    private String deviceId;
    /** 登录 IP 地址。 */
    private String loginIp;
    /** 客户端 User-Agent。 */
    private String userAgent;
    /** 登录时间。 */
    private LocalDateTime loginAt;
    /** 是否为当前会话。 */
    private boolean current;
}

