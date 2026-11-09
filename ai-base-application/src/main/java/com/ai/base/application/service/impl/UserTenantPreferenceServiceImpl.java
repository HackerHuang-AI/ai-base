package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.port.UserPreferencePort;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.application.utils.CurrentUserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserTenantPreferenceServiceImpl implements UserTenantPreferenceService {
    private final UserPreferencePort userPreferencePort;
    private final TenantUserService tenantUserService;

    public UserTenantPreferenceServiceImpl(UserPreferencePort userPreferencePort, TenantUserService tenantUserService) {
        this.userPreferencePort = userPreferencePort;
        this.tenantUserService = tenantUserService;
    }

    @Override
    public UserTenantPreferenceBO getByUserId(String userId) {
        return userPreferencePort.findByUserId(userId).orElse(null);
    }

    @Override
    public UserTenantPreferenceBO getCurrent() {
        return getByUserId(CurrentUserUtil.requireUserId());
    }

    @Override
    public void updateCurrentDefault(String tenantId) {
        String userId = CurrentUserUtil.requireUserId();
        if (tenantId != null) {
            if (tenantId.isBlank()) {
                throw new BizException(ErrorCodeEnum.PARAMETER_FORMAT_INVALID);
            }
            tenantUserService.requireAvailable(userId, tenantId);
        }
        if (!insertIfAbsent(userId, tenantId, null, null)) {
            requireSaved(userPreferencePort.updateDefaultTenantId(userId, tenantId));
        }
    }

    @Override
    public void recordLastUsed(String userId, String tenantId, LocalDateTime usedAt) {
        try {
            if (!insertIfAbsent(userId, null, tenantId, usedAt)) {
                userPreferencePort.updateLastUsed(userId, tenantId, usedAt);
            }
        } catch (RuntimeException e) {
            log.error("记录用户最近使用空间失败, userId={}, tenantId={}", userId, tenantId, e);
        }
    }

    private boolean insertIfAbsent(String userId, String defaultTenantId, String lastUsedTenantId,
                                    LocalDateTime lastUsedAt) {
        if (userPreferencePort.findByUserId(userId).isPresent()) {
            return false;
        }
        return userPreferencePort.insertPreference(userId, defaultTenantId, lastUsedTenantId, lastUsedAt);
    }

    private void requireSaved(boolean saved) {
        if (!saved) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
    }
}
