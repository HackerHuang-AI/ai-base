package com.ai.base.application.config.param;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 线程池参数配置实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.config.param
 * @ClassName: ThreadPoolParam
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
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

