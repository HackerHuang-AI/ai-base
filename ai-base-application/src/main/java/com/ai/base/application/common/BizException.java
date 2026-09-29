package com.ai.base.application.common;

import com.ai.base.application.enums.ErrorCodeEnum;
import lombok.Getter;

import java.util.List;

@Getter
public class BizException extends RuntimeException {
    private final ErrorCodeEnum errorCode;
    private final List<ErrorCodeEnum> errorDetails;
    private final Object[] args;

    public BizException(ErrorCodeEnum errorCode, Object... args) {
        this(errorCode, List.of(), args);
    }

    public BizException(ErrorCodeEnum errorCode, ErrorCodeEnum errorDetail) {
        this(errorCode, List.of(errorDetail));
    }

    public BizException(ErrorCodeEnum errorCode, List<ErrorCodeEnum> errorDetails, Object... args) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.errorDetails = List.copyOf(errorDetails);
        this.args = args;
    }
}

