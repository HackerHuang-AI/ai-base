package com.ai.base.application.port;

import com.ai.base.application.config.BaseAuthConfig;

/**
 * 认证策略配置端口：屏蔽 Nacos 配置读取细节，原始配置由应用层归一化。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface AuthPolicyPort {

    /** 读取认证配置，Nacos 未配置时返回 null。 */
    BaseAuthConfig authConfig();
}
