package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserTenantPreferenceService;
import com.ai.base.application.utils.CurrentUserUtil;
import com.ai.base.infrastructure.persistence.entity.UserTenantPreferenceEntity;
import com.ai.base.infrastructure.persistence.mapper.UserTenantPreferenceMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserTenantPreferenceServiceImpl implements UserTenantPreferenceService {
    private final UserTenantPreferenceMapper preferenceMapper;
    private final TenantUserService tenantUserService;

    public UserTenantPreferenceServiceImpl(UserTenantPreferenceMapper preferenceMapper, TenantUserService tenantUserService) {
        this.preferenceMapper = preferenceMapper;
        this.tenantUserService = tenantUserService;
    }

    @Override
    public UserTenantPreferenceBO getByUserId(String userId) {
        UserTenantPreferenceEntity entity = findByUserId(userId);
        if (entity == null) {
            return null;
        }
        UserTenantPreferenceBO preference = new UserTenantPreferenceBO();
        preference.setUserId(entity.getUserId());
        preference.setDefaultTenantId(entity.getDefaultTenantId());
        preference.setLastUsedTenantId(entity.getLastUsedTenantId());
        return preference;
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
        UserTenantPreferenceEntity entity = new UserTenantPreferenceEntity();
        entity.setUserId(userId);
        entity.setDefaultTenantId(tenantId);
        if (!insertIfAbsent(entity)) {
            requireSaved(preferenceMapper.updateDefaultTenantId(userId, tenantId));
        }
    }

    @Override
    public void recordLastUsed(String userId, String tenantId, LocalDateTime usedAt) {
        try {
            UserTenantPreferenceEntity entity = new UserTenantPreferenceEntity();
            entity.setUserId(userId);
            entity.setLastUsedTenantId(tenantId);
            entity.setLastUsedAt(usedAt);
            if (!insertIfAbsent(entity)) {
                preferenceMapper.updateLastUsedTenantId(userId, tenantId, usedAt);
            }
        } catch (RuntimeException e) {
            log.error("记录用户最近使用空间失败, userId={}, tenantId={}", userId, tenantId, e);
        }
    }

    private UserTenantPreferenceEntity findByUserId(String userId) {
        return preferenceMapper.selectOne(Wrappers.<UserTenantPreferenceEntity>lambdaQuery()
                .eq(UserTenantPreferenceEntity::getUserId, userId));
    }

    private boolean insertIfAbsent(UserTenantPreferenceEntity entity) {
        if (findByUserId(entity.getUserId()) != null) {
            return false;
        }
        try {
            requireSaved(preferenceMapper.insert(entity));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private void requireSaved(int affectedRows) {
        if (affectedRows == 0) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
    }
}

