package com.ai.base.infrastructure.repository;

import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.port.UserTenantRelationPort;
import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户-租户关系查询端口实现。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class UserTenantRelationRepository implements UserTenantRelationPort {

    private final TenantUserExtensionMapper tenantUserExtensionMapper;

    public UserTenantRelationRepository(TenantUserExtensionMapper tenantUserExtensionMapper) {
        this.tenantUserExtensionMapper = tenantUserExtensionMapper;
    }

    @Override
    public Optional<String> findPersonalTenantId(String userId) {
        return Optional.ofNullable(tenantUserExtensionMapper.selectPersonalTenantId(userId));
    }

    @Override
    public Optional<UserTenantBO> findPersonalRelation(String userId) {
        return Optional.ofNullable(tenantUserExtensionMapper.selectPersonalByUserId(userId))
                .map(this::toTenant);
    }

    @Override
    public Optional<UserTenantBO> findAvailableRelation(String userId, String tenantId) {
        return Optional.ofNullable(tenantUserExtensionMapper.selectAvailableByUserIdAndTenantId(userId, tenantId))
                .map(this::toTenant);
    }

    @Override
    public long countAvailable(String userId) {
        return tenantUserExtensionMapper.countAvailableByUserId(userId);
    }

    @Override
    public List<UserTenantBO> pageAvailable(String userId, long offset, int pageSize) {
        return tenantUserExtensionMapper.selectAvailablePageByUserId(userId, offset, pageSize).stream()
                .map(this::toTenant)
                .toList();
    }

    @Override
    public Map<String, List<UserTenantBO>> listRelationsGroupedByUserIds(List<String> userIds) {
        return tenantUserExtensionMapper.selectByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserTenantRow::getUserId,
                        Collectors.mapping(this::toTenant, Collectors.toList())));
    }

    private UserTenantBO toTenant(UserTenantRow row) {
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId(row.getTenantId());
        tenant.setTenantName(row.getTenantName());
        tenant.setTenantType(row.getTenantType());
        tenant.setTenantTypeDescription(TenantTypeEnum.descriptionOf(row.getTenantType()));
        return tenant;
    }
}
