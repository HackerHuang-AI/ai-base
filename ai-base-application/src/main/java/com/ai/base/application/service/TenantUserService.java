package com.ai.base.application.service;

import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserTenantBO;

import java.util.Optional;

public interface TenantUserService {
    BasePageOutBO<UserTenantBO> pageCurrent(Integer pageNo, Integer pageSize);

    UserTenantBO requireAvailable(String userId, String tenantId);

    /** 查询用户在指定租户下的可用关系，不存在时返回空。 */
    Optional<UserTenantBO> findAvailable(String userId, String tenantId);

    /** 查询用户的个人租户标识，不存在时返回空。 */
    Optional<String> findPersonalTenantId(String userId);
}
