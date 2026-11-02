package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantSwitchVO {
    @NotBlank(message = "1001003")
    @Size(max = 128, message = "1001004")
    private String tenantId;
}

