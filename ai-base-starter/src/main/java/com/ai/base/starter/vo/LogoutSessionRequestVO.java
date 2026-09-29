package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 指定会话注销请求参数。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.starter.vo
 * @ClassName: LogoutSessionRequestVO
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
public class LogoutSessionRequestVO {
    /** 待注销的会话标识。 */
    @NotBlank(message = "1001003")
    private String sessionId;
}

