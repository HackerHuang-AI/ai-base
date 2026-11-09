package com.ai.base.application.port;

import com.ai.base.application.bo.UserTenantBO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户-租户关系查询端口：只暴露"可用租户"这一业务语义。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface UserTenantRelationPort {

    /** 查询用户的个人租户标识。 */
    Optional<String> findPersonalTenantId(String userId);

    /** 查询用户的个人租户关系。 */
    Optional<UserTenantBO> findPersonalRelation(String userId);

    /** 查询用户在指定租户下的可用关系。 */
    Optional<UserTenantBO> findAvailableRelation(String userId, String tenantId);

    /** 统计用户可用租户数。 */
    long countAvailable(String userId);

    /** 分页查询用户可用租户。 */
    List<UserTenantBO> pageAvailable(String userId, long offset, int pageSize);

    /** 批量查询多个用户的全部租户关系，按用户标识分组。 */
    Map<String, List<UserTenantBO>> listRelationsGroupedByUserIds(List<String> userIds);
}
