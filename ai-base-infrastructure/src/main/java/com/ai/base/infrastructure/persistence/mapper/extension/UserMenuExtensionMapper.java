package com.ai.base.infrastructure.persistence.mapper.extension;

import com.ai.base.infrastructure.persistence.dto.UserMenuRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMenuExtensionMapper {
    List<UserMenuRow> selectPersonalMenus();

    List<UserMenuRow> selectTenantMenus(@Param("userId") String userId, @Param("tenantId") String tenantId);
}

