package com.ai.base.application.common;

import com.ai.base.application.enums.ErrorCodeEnum;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final ErrorCodeEnum errorCode;
    private final Object[] args;

    public BizException(ErrorCodeEnum errorCode, Object... args) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = args;
    }
}

