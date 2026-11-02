package com.ai.base.infrastructure.persistence.mapper.extension;

import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TenantUserExtensionMapper {
    String selectPersonalTenantId(@Param("userId") String userId);

    UserTenantRow selectPersonalByUserId(@Param("userId") String userId);

    List<UserTenantRow> selectByUserIds(@Param("userIds") List<String> userIds);

    long countAvailableByUserId(@Param("userId") String userId);

    List<UserTenantRow> selectAvailablePageByUserId(@Param("userId") String userId,
                                                  @Param("offset") long offset, @Param("pageSize") int pageSize);

    UserTenantRow selectAvailableByUserIdAndTenantId(@Param("userId") String userId,
                                                    @Param("tenantId") String tenantId);
}

