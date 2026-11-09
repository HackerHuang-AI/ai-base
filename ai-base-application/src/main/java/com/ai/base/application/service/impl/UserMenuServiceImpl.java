package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserMenuBO;
import com.ai.base.application.bo.UserMenuNodeBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.port.UserMenuPort;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.application.service.UserMenuService;
import com.ai.base.application.utils.CurrentUserUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserMenuServiceImpl implements UserMenuService {
    private final TenantUserService tenantUserService;
    private final UserMenuPort userMenuPort;

    public UserMenuServiceImpl(TenantUserService tenantUserService, UserMenuPort userMenuPort) {
        this.tenantUserService = tenantUserService;
        this.userMenuPort = userMenuPort;
    }

    @Override
    public List<UserMenuBO> treeCurrent() {
        String userId = CurrentUserUtil.requireUserId();
        String tenantId = CurrentUserUtil.requireTenantId();
        UserTenantBO tenant = tenantUserService.requireAvailable(userId, tenantId);
        List<UserMenuNodeBO> rows;
        if (TenantTypeEnum.PERSONAL.getValue().equals(tenant.getTenantType())) {
            rows = userMenuPort.findPersonalMenus();
        } else if (TenantTypeEnum.ENTERPRISE.getValue().equals(tenant.getTenantType())) {
            rows = userMenuPort.findTenantMenus(userId, tenantId);
        } else {
            throw new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        }
        Map<Long, List<UserMenuNodeBO>> byParent = new HashMap<>();
        rows.stream().sorted(Comparator.comparing(UserMenuNodeBO::getSortNo).thenComparing(UserMenuNodeBO::getId))
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

    private List<UserMenuBO> buildChildren(Long parentId, Map<Long, List<UserMenuNodeBO>> byParent) {
        List<UserMenuBO> result = new ArrayList<>();
        for (UserMenuNodeBO row : byParent.getOrDefault(parentId, List.of())) {
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
            node.setDesc(row.getDesc());
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
