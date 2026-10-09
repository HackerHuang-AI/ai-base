package com.ai.base.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 公共插件配置。 */
@Configuration
public class MybatisPlusConfig {
    /**
     * 为标注 {@code @Version} 的业务实体启用乐观锁。
     * 仅 VersionedEntity 的子类参与版本校验；审计日志实体不受影响。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}

