package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 角色权限关联实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: RolePermissionEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("role_permission")
public class RolePermissionEntity extends BaseEntity {
    /** 角色 ID。 */
    private Long roleId;
    /** 权限点 ID。 */
    private Long permissionId;
}

