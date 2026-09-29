package com.ai.base.infrastructure.enums;

/**
 * Nacos 业务配置 DataId。
 */
public enum NacosDataIdEnum {

    AI_BASE_AUTH("ai-base-auth.json"),
    AI_BASE_THREAD_POOL("ai-base-thread-pool.json");

    private final String dataId;

    NacosDataIdEnum(String dataId) {
        this.dataId = dataId;
    }

    public String dataId() {
        return dataId;
    }
}

