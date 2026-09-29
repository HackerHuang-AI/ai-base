package com.ai.base.starter.handler;

import com.ai.base.starter.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource());

    @Test
    void missingRequestHeaderShouldReturnValidationError() {
        Result<Void> result = handler.handleMissingRequestHeaderException(
                new MissingRequestHeaderException("X-User-Id", null), Locale.SIMPLIFIED_CHINESE);

        assertThat(result.getCode().getCode()).isEqualTo("01");
        assertThat(result.getErrorCode()).isEqualTo("1001001");
        assertThat(result.getMessage()).isEqualTo("请求参数校验失败");
        assertThat(result.getErrors()).singleElement().satisfies(error -> {
            assertThat(error.errorCode()).isEqualTo("1001003");
            assertThat(error.message()).isEqualTo("参数不能为空");
        });
    }

    @Test
    void genericEnglishLocaleShouldReturnEnglishMessage() {
        Result<Void> result = handler.handleException(new RuntimeException(), Locale.ENGLISH);

        assertThat(result.getMessage()).isEqualTo("System error, please try again later");
    }

    private ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}

