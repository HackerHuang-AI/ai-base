package com.ai.base.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NacosConfig {
    @Value("${spring.cloud.nacos.config.server-addr:}")
    private String serverAddr;

    public boolean isAvailable() {
        return serverAddr != null && !serverAddr.isBlank();
    }
}

