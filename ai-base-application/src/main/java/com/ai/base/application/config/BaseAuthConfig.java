package com.ai.base.application.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 基础认证配置。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.config
 * @ClassName: BaseAuthConfig
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
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

