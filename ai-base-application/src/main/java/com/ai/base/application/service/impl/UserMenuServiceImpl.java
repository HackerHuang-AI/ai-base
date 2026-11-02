package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserMenuBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserMenuService;
import com.ai.base.application.utils.CurrentUserUtil;
import com.ai.base.infrastructure.persistence.dto.UserMenuRow;
import com.ai.base.infrastructure.persistence.mapper.extension.UserMenuExtensionMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserMenuServiceImpl implements UserMenuService {
    private final TenantUserService tenantUserService;
    private final UserMenuExtensionMapper userMenuExtensionMapper;

    public UserMenuServiceImpl(TenantUserService tenantUserService, UserMenuExtensionMapper userMenuExtensionMapper) {
        this.tenantUserService = tenantUserService;
        this.userMenuExtensionMapper = userMenuExtensionMapper;
    }

    @Override
    public List<UserMenuBO> treeCurrent() {
        String userId = CurrentUserUtil.requireUserId();
        String tenantId = CurrentUserUtil.requireTenantId();
        UserTenantBO tenant = tenantUserService.requireAvailable(userId, tenantId);
        List<UserMenuRow> rows;
        if (TenantTypeEnum.PERSONAL.getValue().equals(tenant.getTenantType())) {
            rows = userMenuExtensionMapper.selectPersonalMenus();
        } else if (TenantTypeEnum.ENTERPRISE.getValue().equals(tenant.getTenantType())) {
            rows = userMenuExtensionMapper.selectTenantMenus(userId, tenantId);
        } else {
            throw new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        }
        Map<Long, List<UserMenuRow>> byParent = new HashMap<>();
        rows.stream().sorted(Comparator.comparing(UserMenuRow::getSortNo).thenComparing(UserMenuRow::getId))
                .forEach(row -> byParent.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row));
        List<UserMenuBO> result = new ArrayList<>();
        for (UserMenuBO node : buildChildren(null, byParent)) {
            if ("ROOT".equals(node.getMenuCode()) && node.getResourceType() == 1) {
                result.addAll(node.getChildren());
            } else {
                result.add(node);
            }
        }
        return result;
    }

    private List<UserMenuBO> buildChildren(Long parentId, Map<Long, List<UserMenuRow>> byParent) {
        List<UserMenuBO> result = new ArrayList<>();
        for (UserMenuRow row : byParent.getOrDefault(parentId, List.of())) {
            if (row.getResourceType() == 2 && !row.isAuthorized()) {
                continue;
            }
            List<UserMenuBO> children = buildChildren(row.getId(), byParent);
            if (row.getResourceType() == 1 && children.isEmpty()) {
                continue;
            }
            UserMenuBO node = new UserMenuBO();
            node.setMenuCode(row.getMenuCode());
            node.setResourceName(row.getResourceName());
            node.setResourceType(row.getResourceType());
            node.setRoutePath(row.getRoutePath());
            node.setIcon(row.getIcon());
            node.setSortNo(row.getSortNo());
            node.setChildren(children);
            result.add(node);
        }
        return result;
    }
}

