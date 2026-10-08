package com.ai.base.application.config;

import com.ai.base.application.config.param.ThreadPoolParam;
import com.ai.base.infrastructure.config.NacosConfig;
import com.ai.base.infrastructure.enums.NacosDataIdEnum;
import com.alibaba.nacos.api.config.listener.Listener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务线程池配置与运行时管理。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * 线程池注册表：新增线程池时只需在此声明默认参数，并暴露对应的 Bean。
     * nacosKey 对应 ai-base-thread-pool 配置中的独立配置段。
     */
    enum PoolDef {
        DEFAULT("default-pool", 4, 16, 200, 60),
        LOGIN_AUDIT("login-audit-pool", 2, 4, 1000, 60);

        final String nacosKey;
        final int defaultCore;
        final int defaultMax;
        final int defaultQueue;
        final int defaultKeepAlive;

        PoolDef(String nacosKey, int defaultCore, int defaultMax, int defaultQueue, int defaultKeepAlive) {
            this.nacosKey = nacosKey;
            this.defaultCore = defaultCore;
            this.defaultMax = defaultMax;
            this.defaultQueue = defaultQueue;
            this.defaultKeepAlive = defaultKeepAlive;
        }
    }

    /** 已创建线程池及其拒绝计数，供 Nacos 热更新、指标采集和应用关闭时统一管理。 */
    private final Map<PoolDef, ThreadPoolExecutor> executors = new EnumMap<>(PoolDef.class);
    private final Map<PoolDef, AtomicLong> rejectedCounts = new EnumMap<>(PoolDef.class);

    /** 独立的守护线程，仅用于定时输出各业务线程池的运行指标。 */
    private ScheduledExecutorService statsScheduler;

    @Autowired
    private NacosConfig nacosConfig;

    @Bean("defaultExecutor")
    public ThreadPoolExecutor defaultExecutor() {
        return createAndRegister(PoolDef.DEFAULT);
    }

    @Bean("loginAuditExecutor")
    public ThreadPoolExecutor loginAuditExecutor() {
        return createAndRegister(PoolDef.LOGIN_AUDIT);
    }

    /**
     * 在所有线程池 Bean 创建后订阅 Nacos 配置，并启动定时指标采集。
     * 配置变更时按线程池配置段分别读取和更新，互不影响。
     */
    @PostConstruct
    public void registerNacosListenerAndStartStats() {
        nacosConfig.addListener(NacosDataIdEnum.AI_BASE_THREAD_POOL.dataId(), new Listener() {
            @Override
            public java.util.concurrent.Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("[ThreadPool] 收到 Nacos 配置变更，开始热更新");
                executors.forEach((def, executor) -> hotUpdate(executor, def));
            }
        });

        statsScheduler = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread thread = new Thread(runnable, "ai-base-pool-stats");
                    thread.setDaemon(true);
                    return thread;
                });
        statsScheduler.scheduleAtFixedRate(this::logStats, 60, 60, TimeUnit.SECONDS);
    }

    /** 输出活跃线程、队列积压、已完成任务和累计拒绝数，便于运行时排查容量问题。 */
    private void logStats() {
        log.info("[ThreadPool-Stats] view");
        executors.forEach((def, executor) -> {
            AtomicLong rejected = rejectedCounts.get(def);
            log.info("[ThreadPool-Stats] {} active={}/{} queue={}/{} completed={} rejected={}",
                    def.nacosKey,
                    executor.getActiveCount(), executor.getMaximumPoolSize(),
                    executor.getQueue().size(),
                    ((ResizableLinkedBlockingQueue<?>) executor.getQueue()).getCapacity(),
                    executor.getCompletedTaskCount(),
                    rejected != null ? rejected.get() : 0);
        });
    }

    /** 应用停止时先关闭指标采集器，再依次优雅关闭所有业务线程池。 */
    @PreDestroy
    public void shutdown() {
        if (statsScheduler != null) {
            statsScheduler.shutdownNow();
        }
        executors.forEach((def, executor) -> shutdownExecutor(executor, def.nacosKey));
    }

    private ThreadPoolExecutor createAndRegister(PoolDef def) {
        AtomicLong rejected = new AtomicLong(0);
        rejectedCounts.put(def, rejected);
        ThreadPoolParam param = readParam(def);
        ThreadPoolExecutor executor = buildExecutor(param, def.nacosKey, rejected);
        executors.put(def, executor);
        log.info("[ThreadPool] {} 初始化完成, param={}", def.nacosKey, param);
        return executor;
    }

    /**
     * 使用最新 Nacos 参数原地调整线程池，不丢弃队列中的既有任务。
     * 调整 core/max 时必须根据目标 core 与当前 max 的关系确定先后顺序，
     * 否则 ThreadPoolExecutor 会因 corePoolSize 大于 maximumPoolSize 拒绝更新。
     */
    private void hotUpdate(ThreadPoolExecutor executor, PoolDef def) {
        if (executor == null) {
            return;
        }
        ThreadPoolParam newParam = readParam(def);

        int oldCore = executor.getCorePoolSize();
        int oldMax = executor.getMaximumPoolSize();
        int oldQueue = ((ResizableLinkedBlockingQueue<?>) executor.getQueue()).getCapacity();

        if (oldCore == newParam.getCorePoolSize()
                && oldMax == newParam.getMaxPoolSize()
                && oldQueue == newParam.getQueueCapacity()
                && executor.getKeepAliveTime(TimeUnit.SECONDS) == newParam.getKeepAliveSeconds()) {
            return;
        }

        if (newParam.getCorePoolSize() > executor.getMaximumPoolSize()) {
            executor.setMaximumPoolSize(newParam.getMaxPoolSize());
            executor.setCorePoolSize(newParam.getCorePoolSize());
        } else {
            executor.setCorePoolSize(newParam.getCorePoolSize());
            executor.setMaximumPoolSize(newParam.getMaxPoolSize());
        }
        executor.setKeepAliveTime(newParam.getKeepAliveSeconds(), TimeUnit.SECONDS);
        ((ResizableLinkedBlockingQueue<?>) executor.getQueue()).setCapacity(newParam.getQueueCapacity());

        log.info("[ThreadPool] {} 热更新完成: core {} -> {}, max {} -> {}, queue {} -> {}",
                def.nacosKey, oldCore, newParam.getCorePoolSize(),
                oldMax, newParam.getMaxPoolSize(),
                oldQueue, newParam.getQueueCapacity());
    }

    private void shutdownExecutor(ThreadPoolExecutor executor, String name) {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                log.warn("[ThreadPool] {} 等待超时，强制终止", name);
            } else {
                log.info("[ThreadPool] {} 已优雅关闭", name);
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 读取指定线程池的独立配置；配置缺失时使用枚举中约定的默认值，保证应用可启动。
     */
    private ThreadPoolParam readParam(PoolDef def) {
        ThreadPoolParam param = nacosConfig.getObject(
                NacosDataIdEnum.AI_BASE_THREAD_POOL, def.nacosKey, ThreadPoolParam.class);
        if (param == null) {
            log.warn("[ThreadPool] Nacos 未配置 {}，使用默认参数 core={} max={} queue={} keepAlive={}s",
                    def.nacosKey, def.defaultCore, def.defaultMax, def.defaultQueue, def.defaultKeepAlive);
            param = new ThreadPoolParam();
            param.setCorePoolSize(def.defaultCore);
            param.setMaxPoolSize(def.defaultMax);
            param.setQueueCapacity(def.defaultQueue);
            param.setKeepAliveSeconds(def.defaultKeepAlive);
        }
        return param;
    }

    private ThreadPoolExecutor buildExecutor(ThreadPoolParam param, String poolName, AtomicLong rejectedCount) {
        ResizableLinkedBlockingQueue<Runnable> queue = new ResizableLinkedBlockingQueue<>(param.getQueueCapacity(), poolName);
        return new ThreadPoolExecutor(
                param.getCorePoolSize(),
                param.getMaxPoolSize(),
                param.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                queue,
                new NamedThreadFactory(poolName),
                (runnable, executor) -> {
                    rejectedCount.incrementAndGet();
                    log.error("[ThreadPool] {} 线程池已满，拒绝任务: active={}/{}, queue={}/{}",
                            poolName,
                            executor.getActiveCount(), executor.getMaximumPoolSize(),
                            executor.getQueue().size(),
                            ((ResizableLinkedBlockingQueue<?>) executor.getQueue()).getCapacity());
                    throw new RejectedExecutionException("线程池已满: " + poolName);
                });
    }

    /**
     * 底层队列使用无界容量以规避 LinkedBlockingQueue 容量不可变的限制，
     * 再通过 offer 按当前 capacity 实施逻辑容量控制，从而支持 Nacos 热更新。
     */
    private static class ResizableLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {
        private volatile int capacity;
        private final String poolName;

        ResizableLinkedBlockingQueue(int capacity, String poolName) {
            super(Integer.MAX_VALUE);
            this.capacity = capacity;
            this.poolName = poolName;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int newCapacity) {
            if (newCapacity <= 0) {
                throw new IllegalArgumentException("队列容量必须大于 0，实际值: " + newCapacity);
            }
            int old = this.capacity;
            this.capacity = newCapacity;
            log.info("[ResizableQueue] {} 队列容量变更: {} -> {}", poolName, old, newCapacity);
        }

        @Override
        public boolean offer(E element) {
            return size() < capacity && super.offer(element);
        }
    }

    /** 为不同业务池生成可辨识的线程名，方便通过线程栈和日志定位任务来源。 */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        NamedThreadFactory(String poolName) {
            this.namePrefix = poolName + "-thread-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}

