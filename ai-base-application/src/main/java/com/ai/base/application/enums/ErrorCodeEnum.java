package com.ai.base.application.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    PARAM_ILLEGAL("1101001", "error.1101001", "参数不合法"),
    SYSTEM_ERROR("1101002", "error.1101002", "系统异常，请稍后重试");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

    ErrorCodeEnum(String code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }
}

