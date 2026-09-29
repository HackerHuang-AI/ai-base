package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description: 组织用户角色授权实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: OrganizationUserRoleEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("organization_user_role")
public class OrganizationUserRoleEntity extends BaseEntity {
    /** 全局租户 ID。 */
    private String tenantId;
    /** 全局用户 ID。 */
    private String userId;
    /** 组织信息 ID。 */
    private Long organizationId;
    /** 角色 ID。 */
    private Long roleId;
    /** 授权生效时间。 */
    private LocalDateTime effectiveStartTime;
    /** 授权失效时间，NULL 表示当前有效。 */
    private LocalDateTime effectiveEndTime;
}

