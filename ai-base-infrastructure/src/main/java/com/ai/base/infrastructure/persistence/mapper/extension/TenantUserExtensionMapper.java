package com.ai.base.infrastructure.persistence.mapper.extension;

import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TenantUserExtensionMapper {
    String selectPersonalTenantId(@Param("userId") String userId);

    UserTenantRow selectPersonalByUserId(@Param("userId") String userId);

    List<UserTenantRow> selectByUserIds(@Param("userIds") List<String> userIds);
}

