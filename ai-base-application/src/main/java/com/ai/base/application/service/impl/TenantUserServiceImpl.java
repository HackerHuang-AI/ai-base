package com.ai.base.application.service.impl;

import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.port.UserTenantRelationPort;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.utils.CurrentUserUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantUserServiceImpl implements TenantUserService {
    private final UserTenantRelationPort userTenantRelationPort;

    public TenantUserServiceImpl(UserTenantRelationPort userTenantRelationPort) {
        this.userTenantRelationPort = userTenantRelationPort;
    }

    @Override
    public BasePageOutBO<UserTenantBO> pageCurrent(Integer pageNo, Integer pageSize) {
        if (pageNo == null || pageNo < 1) {
            throw new BizException(ErrorCodeEnum.PAGE_NUMBER_OUT_OF_RANGE);
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new BizException(ErrorCodeEnum.PAGE_SIZE_OUT_OF_RANGE);
        }
        String userId = CurrentUserUtil.requireUserId();
        BasePageOutBO<UserTenantBO> result = new BasePageOutBO<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(userTenantRelationPort.countAvailable(userId));
        result.setRecords(userTenantRelationPort.pageAvailable(userId, (pageNo - 1L) * pageSize, pageSize));
        return result;
    }

    @Override
    public UserTenantBO requireAvailable(String userId, String tenantId) {
        if (!tenantId.matches("[a-z0-9_-]+")) {
            throw new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        }
        return findAvailable(userId, tenantId)
                .filter(tenant -> tenant.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE));
    }

    @Override
    public Optional<UserTenantBO> findAvailable(String userId, String tenantId) {
        return userTenantRelationPort.findAvailableRelation(userId, tenantId);
    }

    @Override
    public Optional<String> findPersonalTenantId(String userId) {
        return userTenantRelationPort.findPersonalTenantId(userId);
    }
}
