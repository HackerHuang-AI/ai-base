package com.ai.base.starter.common;

import com.ai.base.application.enums.ErrorCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successShouldOnlyContainCodeMessageAndData() throws Exception {
        String json = objectMapper.writeValueAsString(Result.success("data"));

        assertThat(json).isEqualTo("{\"code\":\"00\",\"message\":\"成功\",\"data\":\"data\"}");
    }

    @Test
    void errorShouldContainErrorCodeAndErrors() throws Exception {
        Result<Void> result = Result.error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, "请求参数校验失败",
                List.of(new ErrorItem("1101101", "登录方式不能为空")));

        String json = objectMapper.writeValueAsString(result);

        assertThat(json).isEqualTo("{\"code\":\"01\",\"message\":\"请求参数校验失败\",\"errorCode\":\"1001001\",\"data\":null,\"errors\":[{\"errorCode\":\"1101101\",\"message\":\"登录方式不能为空\"}]}");
    }
}

