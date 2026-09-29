package com.ai.base.application.enums;

import com.ai.base.application.common.BizException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum LoginTypeEnum {
    /** 使用本地账号和密码认证，对应 ACCOUNT + LOCAL 身份。 */
    ACCOUNT_PASSWORD("ACCOUNT_PASSWORD"),
    /** 使用手机号和短信验证码认证，对应 MOBILE + SMS 身份。 */
    MOBILE_SMS("MOBILE_SMS"),
    /** 使用 App 扫码确认认证，当前暂未开放。 */
    APP_QR_CONFIRM("APP_QR_CONFIRM"),
    /** 仅用于早期测试的手机号模拟认证，对应 MOBILE + MOCK 身份。 */
    MOCK("MOCK");

    private final String value;

    LoginTypeEnum(String value) {
        this.value = value;
    }

    public static LoginTypeEnum fromValue(String value) {
        return Arrays.stream(values())
                .filter(loginType -> loginType.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.REQUEST_VALIDATION_FAILED, ErrorCodeEnum.LOGIN_TYPE_INVALID));
    }
}

