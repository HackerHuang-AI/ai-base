package com.ai.base.application.port;

import com.ai.base.application.bo.AdminUserQueryBO;
import com.ai.base.application.bo.UserProfileBO;

import java.util.List;

/**
 * 用户画像查询端口：关联查询与管理端分页，屏蔽 SQL 细节。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface UserProfileQueryPort {

    /** 批量查询用户画像，结果按 user_id 去重。 */
    List<UserProfileBO> findProfilesByUserIds(List<String> userIds);

    /** 管理端用户总数统计。 */
    long countUsers(AdminUserQueryBO query);

    /** 管理端用户分页查询。 */
    List<UserProfileBO> pageUsers(AdminUserQueryBO query, long offset, int pageSize);
}
