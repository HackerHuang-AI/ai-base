package com.ai.base.application.port;

import com.ai.base.application.bo.UserMenuNodeBO;

import java.util.List;

/**
 * 用户菜单查询端口：返回扁平节点，由应用层组装树。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface UserMenuPort {

    /** 查询个人空间菜单。 */
    List<UserMenuNodeBO> findPersonalMenus();

    /** 查询企业空间内指定用户的菜单。 */
    List<UserMenuNodeBO> findTenantMenus(String userId, String tenantId);
}
