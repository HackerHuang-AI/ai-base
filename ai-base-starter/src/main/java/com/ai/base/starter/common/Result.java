package com.ai.base.starter.common;

import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.ResultCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private ResultCodeEnum code;
    private String message;
    private String errorCode;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS, ResultCodeEnum.SUCCESS.getDefaultMessage(), null, data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCodeEnum.ERROR, message, null, null);
    }

    public static <T> Result<T> error(ErrorCodeEnum errorCode, String message) {
        return new Result<>(ResultCodeEnum.ERROR, message, errorCode.getCode(), null);
    }
}

