package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("organization_user_role")
public class OrganizationUserRoleEntity extends BaseEntity {
    /** 组织任职关系 ID。 */
    private Long organizationUserId;
    /** 角色 ID。 */
    private Long roleId;
    /** 授权生效时间。 */
    private LocalDateTime effectiveStartTime;
    /** 授权失效时间，NULL 表示当前有效。 */
    private LocalDateTime effectiveEndTime;
}

