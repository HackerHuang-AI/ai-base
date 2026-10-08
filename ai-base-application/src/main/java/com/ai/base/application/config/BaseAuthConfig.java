package com.ai.base.application.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 基础认证配置。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class BaseAuthConfig {
    /** 会话配置。 */
    private SessionConfig session;

    @Getter
    @Setter
    public static class SessionConfig {
        /** 单用户允许的最大登录设备数。 */
        private Integer maxDevices;
    }
}

