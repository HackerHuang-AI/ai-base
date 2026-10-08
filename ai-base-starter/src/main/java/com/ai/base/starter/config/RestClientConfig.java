package com.ai.base.starter.config;

import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.common.TraceContextSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP 客户端与请求上下文透传配置。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Configuration
public class RestClientConfig {
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String USER_ID_HEADER = "X-Internal-User-Id";
    private static final String TENANT_ID_HEADER = "X-Internal-Tenant-Id";
    private static final String SESSION_ID_HEADER = "X-Internal-Session-Id";
    private static final String CALL_CHAIN_HEADER = "X-Call-Chain";

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public RestClient restClient(HttpClient httpClient) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .requestInterceptor((request, body, execution) -> {
                    RequestContextHolder.current().ifPresent(context -> propagate(request, context));
                    return execution.execute(request, body);
                })
                .build();
    }

    private void propagate(org.springframework.http.HttpRequest request, RequestContext context) {
        request.getHeaders().set(TRACEPARENT_HEADER, TraceContextSupport.childTraceparent(context.traceparent()));
        setHeader(request, USER_ID_HEADER, context.userId());
        setHeader(request, TENANT_ID_HEADER, context.tenantId());
        setHeader(request, SESSION_ID_HEADER, context.sessionId());
        setHeader(request, CALL_CHAIN_HEADER, context.callChain());
    }

    private void setHeader(org.springframework.http.HttpRequest request, String name, String value) {
        if (value != null && !value.isBlank()) {
            request.getHeaders().set(name, value);
        }
    }
}

