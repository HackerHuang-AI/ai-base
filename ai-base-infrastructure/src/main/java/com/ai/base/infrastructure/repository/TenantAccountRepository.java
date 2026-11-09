package com.ai.base.infrastructure.repository;

import com.ai.base.application.bo.TenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.port.TenantAccountPort;
import com.ai.base.infrastructure.persistence.entity.TenantEntity;
import com.ai.base.infrastructure.persistence.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 租户账号存取端口实现。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class TenantAccountRepository implements TenantAccountPort {

    private final TenantMapper tenantMapper;

    public TenantAccountRepository(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @Override
    public Optional<TenantBO> findPersonalTenant(String tenantId) {
        return Optional.ofNullable(tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
                        .eq(TenantEntity::getTenantId, tenantId)
                        .eq(TenantEntity::getTenantType, TenantTypeEnum.PERSONAL.getValue())
                        .eq(TenantEntity::getStatus, 1)))
                .map(this::toTenant);
    }

    @Override
    public void renameTenant(String tenantId, String tenantName) {
        TenantEntity tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
        tenant.setTenantName(tenantName);
        if (tenantMapper.updateById(tenant) == 0) {
            throw new BizException(ErrorCodeEnum.DATA_VERSION_CONFLICT);
        }
    }

    private TenantBO toTenant(TenantEntity entity) {
        TenantBO tenant = new TenantBO();
        tenant.setTenantId(entity.getTenantId());
        tenant.setTenantName(entity.getTenantName());
        tenant.setTenantType(entity.getTenantType());
        tenant.setStatus(entity.getStatus());
        return tenant;
    }
}
