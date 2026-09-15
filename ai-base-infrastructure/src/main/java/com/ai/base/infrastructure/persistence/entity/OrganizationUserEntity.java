package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("organization_user")
public class OrganizationUserEntity extends BaseEntity {
    /** 所属租户 ID。 */
    private Long tenantId;
    /** 组织信息 ID。 */
    private Long organizationId;
    /** 租户成员关系 ID。 */
    private Long tenantUserId;
    /** 岗位 ID。 */
    private Long positionId;
    /** 任职生效时间。 */
    private LocalDateTime effectiveStartTime;
    /** 任职失效时间，NULL 表示当前有效。 */
    private LocalDateTime effectiveEndTime;
    /** 状态：1-在职，2-已结束。 */
    private Integer status;
    /** 创建人用户 ID。 */
    private Long creatorUserId;
    /** 最后修改人用户 ID。 */
    private Long updaterUserId;
}

