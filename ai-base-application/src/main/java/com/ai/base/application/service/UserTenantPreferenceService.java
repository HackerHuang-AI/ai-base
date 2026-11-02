package com.ai.base.application.service;

import com.ai.base.application.bo.UserTenantPreferenceBO;

import java.time.LocalDateTime;

public interface UserTenantPreferenceService {
    UserTenantPreferenceBO getByUserId(String userId);

    UserTenantPreferenceBO getCurrent();

    void updateCurrentDefault(String tenantId);

    void recordLastUsed(String userId, String tenantId, LocalDateTime usedAt);
}

