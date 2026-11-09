package com.ai.base.application.bo;

import lombok.Data;

/**
 * 租户账号（tenant 表语义字段）。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Data
public class TenantBO {
    private String tenantId;
    private String tenantName;
    private Integer tenantType;
    private Integer status;
}
