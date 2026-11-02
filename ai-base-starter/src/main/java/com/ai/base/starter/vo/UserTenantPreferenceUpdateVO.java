package com.ai.base.starter.vo;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserTenantPreferenceUpdateVO {
    @Size(max = 128, message = "1001004")
    private String defaultTenantId;
}

