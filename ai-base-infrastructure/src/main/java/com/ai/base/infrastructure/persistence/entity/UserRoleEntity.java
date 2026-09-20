package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_role")
public class UserRoleEntity extends BaseEntity {
    /** 全局租户 ID。 */
    private String tenantId;
    /** 全局用户 ID。 */
    private String userId;
    /** 授权生效时间。 */
    private LocalDateTime effectiveStartTime;
    /** 授权失效时间，NULL 表示当前有效。 */
    private LocalDateTime effectiveEndTime;
    /** 角色 ID。 */
    private Long roleId;
}

