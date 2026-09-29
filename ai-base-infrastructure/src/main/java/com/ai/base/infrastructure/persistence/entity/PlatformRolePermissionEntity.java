package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 平台角色权限关联实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: PlatformRolePermissionEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("platform_role_permission")
public class PlatformRolePermissionEntity extends BaseEntity {
    /** 平台角色 ID。 */
    private Long platformRoleId;
    /** 权限点 ID。 */
    private Long permissionId;
}

