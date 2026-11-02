package com.ai.base.infrastructure.persistence.mapper;

import com.ai.base.infrastructure.persistence.entity.UserTenantPreferenceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface UserTenantPreferenceMapper extends BaseMapper<UserTenantPreferenceEntity> {
    int updateDefaultTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);

    int updateLastUsedTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId,
                               @Param("usedAt") LocalDateTime usedAt);
}

