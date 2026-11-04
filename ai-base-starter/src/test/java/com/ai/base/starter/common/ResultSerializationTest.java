package com.ai.base.starter.common;

import com.ai.base.application.bo.BasePageOutBO;
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
    void pageShouldOmitUnsetOptionalFields() {
        BasePageOutBO<String> page = new BasePageOutBO<>();
        page.setPageNo(1);
        page.setPageSize(10);
        page.setRecords(List.of());

        var data = objectMapper.valueToTree(Result.success(page)).get("data");

        assertThat(data.size()).isEqualTo(3);
        assertThat(data.get("pageNo").intValue()).isEqualTo(1);
        assertThat(data.get("pageSize").intValue()).isEqualTo(10);
        assertThat(data.has("total")).isFalse();
        assertThat(data.has("totalPages")).isFalse();
        assertThat(data.has("hasNext")).isFalse();
    }

    @Test
    void pageShouldIncludeOptionalFieldsWhenSet() {
        BasePageOutBO<String> page = new BasePageOutBO<>();
        page.setTotal(4_294_967_296L);
        page.setTotalPages(4_294_967_296L);
        page.setHasNext(true);

        var data = objectMapper.valueToTree(Result.success(page)).get("data");

        assertThat(data.get("total").longValue()).isEqualTo(4_294_967_296L);
        assertThat(data.get("totalPages").longValue()).isEqualTo(4_294_967_296L);
        assertThat(data.get("hasNext").booleanValue()).isTrue();

        page.setTotal(0L);
        page.setTotalPages(0L);
        page.setHasNext(false);
        data = objectMapper.valueToTree(Result.success(page)).get("data");

        assertThat(data.get("total").longValue()).isZero();
        assertThat(data.get("totalPages").longValue()).isZero();
        assertThat(data.get("hasNext").booleanValue()).isFalse();
    }

    @Test
    void errorShouldContainErrorCodeAndErrors() throws Exception {
        Result<Void> result = Result.error(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, "请求参数校验失败",
                List.of(new ErrorItem("1101101", "登录方式不能为空")));

        String json = objectMapper.writeValueAsString(result);

        assertThat(json).isEqualTo("{\"code\":\"01\",\"message\":\"请求参数校验失败\",\"errorCode\":\"1001001\",\"data\":null,\"errors\":[{\"errorCode\":\"1101101\",\"message\":\"登录方式不能为空\"}]}");
    }
}

