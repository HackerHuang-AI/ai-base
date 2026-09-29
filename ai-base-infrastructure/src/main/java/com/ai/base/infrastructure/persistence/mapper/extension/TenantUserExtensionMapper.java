package com.ai.base.infrastructure.persistence.mapper.extension;

import org.apache.ibatis.annotations.Param;

public interface TenantUserExtensionMapper {
    String selectPersonalTenantId(@Param("userId") String userId);
}

