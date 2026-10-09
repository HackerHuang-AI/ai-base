package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 修改当前用户账号名的请求参数。 */
@Getter
@Setter
public class PersonalUsernameUpdateVO {
    /** 新账号名。 */
    @NotBlank(message = "1001003")
    private String username;
}

