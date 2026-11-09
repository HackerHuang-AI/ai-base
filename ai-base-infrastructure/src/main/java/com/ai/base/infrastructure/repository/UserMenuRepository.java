package com.ai.base.infrastructure.repository;

import com.ai.base.application.bo.UserMenuNodeBO;
import com.ai.base.application.port.UserMenuPort;
import com.ai.base.infrastructure.persistence.dto.UserMenuRow;
import com.ai.base.infrastructure.persistence.mapper.extension.UserMenuExtensionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户菜单查询端口实现。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class UserMenuRepository implements UserMenuPort {

    private final UserMenuExtensionMapper userMenuExtensionMapper;

    public UserMenuRepository(UserMenuExtensionMapper userMenuExtensionMapper) {
        this.userMenuExtensionMapper = userMenuExtensionMapper;
    }

    @Override
    public List<UserMenuNodeBO> findPersonalMenus() {
        return userMenuExtensionMapper.selectPersonalMenus().stream()
                .map(this::toNode)
                .toList();
    }

    @Override
    public List<UserMenuNodeBO> findTenantMenus(String userId, String tenantId) {
        return userMenuExtensionMapper.selectTenantMenus(userId, tenantId).stream()
                .map(this::toNode)
                .toList();
    }

    private UserMenuNodeBO toNode(UserMenuRow row) {
        UserMenuNodeBO node = new UserMenuNodeBO();
        node.setId(row.getId());
        node.setParentId(row.getParentId());
        node.setMenuCode(row.getMenuCode());
        node.setResourceName(row.getResourceName());
        node.setDesc(row.getDesc());
        node.setResourceType(row.getResourceType());
        node.setRoutePath(row.getRoutePath());
        node.setIcon(row.getIcon());
        node.setSortNo(row.getSortNo());
        node.setAuthorized(row.isAuthorized());
        return node;
    }
}
