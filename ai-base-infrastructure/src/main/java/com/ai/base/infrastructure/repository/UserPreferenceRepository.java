package com.ai.base.infrastructure.repository;

import com.ai.base.application.bo.UserTenantPreferenceBO;
import com.ai.base.application.port.UserPreferencePort;
import com.ai.base.infrastructure.persistence.entity.UserTenantPreferenceEntity;
import com.ai.base.infrastructure.persistence.mapper.UserTenantPreferenceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户租户偏好存取端口实现。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class UserPreferenceRepository implements UserPreferencePort {

    private final UserTenantPreferenceMapper preferenceMapper;

    public UserPreferenceRepository(UserTenantPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    @Override
    public Optional<UserTenantPreferenceBO> findByUserId(String userId) {
        return Optional.ofNullable(preferenceMapper.selectOne(new LambdaQueryWrapper<UserTenantPreferenceEntity>()
                        .eq(UserTenantPreferenceEntity::getUserId, userId)))
                .map(this::toPreference);
    }

    @Override
    public boolean insertPreference(String userId, String defaultTenantId, String lastUsedTenantId,
                                    LocalDateTime lastUsedAt) {
        try {
            UserTenantPreferenceEntity entity = new UserTenantPreferenceEntity();
            entity.setUserId(userId);
            entity.setDefaultTenantId(defaultTenantId);
            entity.setLastUsedTenantId(lastUsedTenantId);
            entity.setLastUsedAt(lastUsedAt);
            return preferenceMapper.insert(entity) > 0;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public boolean updateDefaultTenantId(String userId, String tenantId) {
        return preferenceMapper.updateDefaultTenantId(userId, tenantId) > 0;
    }

    @Override
    public boolean updateLastUsed(String userId, String tenantId, LocalDateTime usedAt) {
        return preferenceMapper.updateLastUsedTenantId(userId, tenantId, usedAt) > 0;
    }

    private UserTenantPreferenceBO toPreference(UserTenantPreferenceEntity entity) {
        UserTenantPreferenceBO preference = new UserTenantPreferenceBO();
        preference.setUserId(entity.getUserId());
        preference.setDefaultTenantId(entity.getDefaultTenantId());
        preference.setLastUsedTenantId(entity.getLastUsedTenantId());
        return preference;
    }
}
