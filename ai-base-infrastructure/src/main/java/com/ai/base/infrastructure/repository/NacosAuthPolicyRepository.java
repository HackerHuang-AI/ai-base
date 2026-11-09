package com.ai.base.infrastructure.repository;

import com.ai.base.application.config.BaseAuthConfig;
import com.ai.base.application.port.AuthPolicyPort;
import com.ai.base.infrastructure.config.NacosConfig;
import com.ai.base.infrastructure.enums.NacosDataIdEnum;
import org.springframework.stereotype.Component;

/**
 * 认证策略配置端口实现：从 Nacos 读取 AI_BASE_AUTH 配置。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class NacosAuthPolicyRepository implements AuthPolicyPort {

    private final NacosConfig nacosConfig;

    public NacosAuthPolicyRepository(NacosConfig nacosConfig) {
        this.nacosConfig = nacosConfig;
    }

    @Override
    public BaseAuthConfig authConfig() {
        return nacosConfig.getDataIdAsObject(NacosDataIdEnum.AI_BASE_AUTH, BaseAuthConfig.class);
    }
}
