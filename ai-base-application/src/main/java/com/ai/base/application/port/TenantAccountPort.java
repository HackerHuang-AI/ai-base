package com.ai.base.application.port;

import com.ai.base.application.bo.TenantBO;

import java.util.Optional;

/**
 * 租户账号存取端口。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface TenantAccountPort {

    /** 查询生效（status=1）的个人租户。 */
    Optional<TenantBO> findPersonalTenant(String tenantId);

    /** 重命名租户，记录不存在或版本冲突时抛出业务异常。 */
    void renameTenant(String tenantId, String tenantName);
}
