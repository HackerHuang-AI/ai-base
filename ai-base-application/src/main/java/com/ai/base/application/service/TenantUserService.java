package com.ai.base.application.service;

import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserTenantBO;

public interface TenantUserService {
    BasePageOutBO<UserTenantBO> pageCurrent(Integer pageNo, Integer pageSize);

    UserTenantBO requireAvailable(String userId, String tenantId);
}

