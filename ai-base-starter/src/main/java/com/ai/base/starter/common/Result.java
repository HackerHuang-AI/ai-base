package com.ai.base.starter.common;

import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.ResultCodeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一接口响应体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /** 响应状态码。 */
    private ResultCodeEnum code;
    /** 响应消息。 */
    private String message;
    /** 业务错误码。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    /** 响应数据。 */
    private T data;
    /** 结构化错误明细。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ErrorItem> errors;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS, ResultCodeEnum.SUCCESS.getDefaultMessage(), null, data, null);
    }

    public static <T> Result<T> error(ErrorCodeEnum errorCode, String message) {
        return error(errorCode, message, List.of());
    }

    public static <T> Result<T> error(ErrorCodeEnum errorCode, String message, List<ErrorItem> errors) {
        return new Result<>(ResultCodeEnum.ERROR, message, errorCode.getCode(), null, List.copyOf(errors));
    }
}

