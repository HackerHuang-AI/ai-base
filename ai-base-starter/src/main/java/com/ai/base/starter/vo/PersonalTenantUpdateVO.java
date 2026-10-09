package com.ai.base.starter.vo;

import lombok.Getter;
import lombok.Setter;

/** 修改当前个人空间名称的请求参数。 */
@Getter
@Setter
public class PersonalTenantUpdateVO {
    /** 个人空间名称。 */
    private String tenantName;
}

