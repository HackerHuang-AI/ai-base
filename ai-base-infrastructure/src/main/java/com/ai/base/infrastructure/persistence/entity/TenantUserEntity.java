package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("tenant_user")
public class TenantUserEntity extends BaseEntity {
    /** 全局租户 ID。 */
    private String tenantId;
    /** 全局用户 ID。 */
    private String userId;
    /** 状态：1-正常，2-已移除，3-待接受。 */
    private Integer status;
    /** 加入时间。 */
    private LocalDateTime joinedAt;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

