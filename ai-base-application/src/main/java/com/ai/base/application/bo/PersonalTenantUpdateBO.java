package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 当前个人空间名称更新参数。 */
@Getter
@Setter
public class PersonalTenantUpdateBO {
    /** 个人空间名称。 */
    private String tenantName;
}

