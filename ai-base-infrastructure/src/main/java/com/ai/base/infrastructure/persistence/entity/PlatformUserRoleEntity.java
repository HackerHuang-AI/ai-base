package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("platform_user_role")
public class PlatformUserRoleEntity extends BaseEntity {
    /** 用户 ID。 */
    private Long userId;
    /** 平台角色 ID。 */
    private Long platformRoleId;
    /** 授权生效时间。 */
    private LocalDateTime effectiveStartTime;
    /** 授权失效时间，NULL 表示当前有效。 */
    private LocalDateTime effectiveEndTime;
}

