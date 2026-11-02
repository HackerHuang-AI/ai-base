package com.ai.base.application.service.impl;

import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.utils.CurrentUserUtil;
import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import org.springframework.stereotype.Service;

@Service
public class TenantUserServiceImpl implements TenantUserService {
    private final TenantUserExtensionMapper tenantUserExtensionMapper;

    public TenantUserServiceImpl(TenantUserExtensionMapper tenantUserExtensionMapper) {
        this.tenantUserExtensionMapper = tenantUserExtensionMapper;
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
        result.setTotal(tenantUserExtensionMapper.countAvailableByUserId(userId));
        result.setRecords(tenantUserExtensionMapper.selectAvailablePageByUserId(userId,
                        (pageNo - 1L) * pageSize, pageSize).stream()
                .map(this::toTenant)
                .toList());
        return result;
    }

    @Override
    public UserTenantBO requireAvailable(String userId, String tenantId) {
        UserTenantRow row = tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(userId, tenantId);
        if (row == null || !row.getTenantId().equals(tenantId) || !tenantId.matches("[a-z0-9_-]+")) {
            throw new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        }
        return toTenant(row);
    }

    private UserTenantBO toTenant(UserTenantRow row) {
        UserTenantBO result = new UserTenantBO();
        result.setTenantId(row.getTenantId());
        result.setTenantName(row.getTenantName());
        result.setTenantType(row.getTenantType());
        result.setTenantTypeDescription(TenantTypeEnum.descriptionOf(row.getTenantType()));
        return result;
    }
}

