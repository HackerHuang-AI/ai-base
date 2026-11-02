package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserTenantPreferenceBO {
    private String userId;
    private String defaultTenantId;
    private String lastUsedTenantId;
}

