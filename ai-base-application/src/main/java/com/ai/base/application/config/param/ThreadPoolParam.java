package com.ai.base.application.config.param;

import lombok.Getter;
import lombok.Setter;

/**
 * 线程池参数配置实体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
public class ThreadPoolParam {
    /** 核心线程数。 */
    private Integer corePoolSize;
    /** 最大线程数。 */
    private Integer maxPoolSize;
    /** 等待队列容量。 */
    private Integer queueCapacity;
    /** 非核心线程空闲存活时间（秒）。 */
    private Integer keepAliveSeconds;
}

