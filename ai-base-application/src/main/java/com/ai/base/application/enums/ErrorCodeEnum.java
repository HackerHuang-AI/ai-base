package com.ai.base.application.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ErrorCodeEnum {
    REQUEST_VALIDATION_FAILED("1001001", "error.1001001", "请求参数校验失败"),
    REQUEST_BODY_INVALID("1001002", "error.1001002", "请求体格式错误"),
    PARAMETER_REQUIRED("1001003", "error.1001003", "参数不能为空"),
    PARAMETER_FORMAT_INVALID("1001004", "error.1001004", "参数格式不合法"),
    PARAMETER_OUT_OF_RANGE("1001005", "error.1001005", "参数超出允许范围"),
    PARAMETER_VALUE_INVALID("1001006", "error.1001006", "参数值不合法"),
    MOBILE_REQUIRED("1001101", "error.1001101", "手机号不能为空"),
    SYSTEM_ERROR("1001999", "error.1001999", "系统异常，请稍后重试"),
    LOGIN_FAILED("1101001", "error.1101001", "账号或密码错误"),
    SMS_CODE_INVALID("1101002", "error.1101002", "验证码错误或已过期"),
    SMS_CODE_SEND_TOO_FREQUENT("1101003", "error.1101003", "验证码发送过于频繁"),
    SMS_SERVICE_UNAVAILABLE("1101004", "error.1101004", "短信服务暂不可用"),
    LOGIN_TYPE_NOT_AVAILABLE("1101005", "error.1101005", "当前登录方式暂未开放"),
    PERSONAL_TENANT_NOT_FOUND("1101006", "error.1101006", "当前用户无可用个人空间"),
    DEVICE_LIMIT_EXCEEDED("1101007", "error.1101007", "当前在线设备数量已达上限，请先下线其他设备"),
    LOGIN_TYPE_REQUIRED("1101101", "error.1101101", "登录方式不能为空"),
    LOGIN_TYPE_INVALID("1101102", "error.1101102", "登录方式不合法"),
    ACCOUNT_REQUIRED("1101103", "error.1101103", "账号不能为空"),
    PASSWORD_REQUIRED("1101104", "error.1101104", "密码不能为空"),
    SMS_CODE_REQUIRED("1101105", "error.1101105", "验证码不能为空"),
    QR_LOGIN_ID_REQUIRED("1101106", "error.1101106", "二维码登录标识不能为空"),
    DEVICE_ID_REQUIRED("1101107", "error.1101107", "设备标识不能为空"),
    DEVICE_ID_FORMAT_INVALID("1101108", "error.1101108", "设备标识格式不合法"),
    USER_NOT_FOUND("1201001", "error.1201001", "用户不存在或已禁用"),
    DATA_VERSION_CONFLICT("1201002", "error.1201002", "数据已被更新，请刷新后重试"),
    CREDENTIAL_ALREADY_IN_USE("1201003", "error.1201003", "账号或手机号已被使用"),
    USER_ID_REQUIRED("1201101", "error.1201101", "用户标识不能为空"),
    PAGE_NUMBER_OUT_OF_RANGE("1201102", "error.1201102", "页码超出允许范围"),
    PAGE_SIZE_OUT_OF_RANGE("1201103", "error.1201103", "每页条数超出允许范围");

    private final String code;
    private final String messageKey;
    private final String defaultMessage;

    ErrorCodeEnum(String code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    public static ErrorCodeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown error code: " + code));
    }
}

