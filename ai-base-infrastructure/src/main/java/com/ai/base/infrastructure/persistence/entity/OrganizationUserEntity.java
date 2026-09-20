package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("organization_user")
public class OrganizationUserEntity extends BaseEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 组织信息 ID。 */
    private Long organizationId;
    /** 全局用户 ID。 */
    private String userId;
    /** 岗位 ID。 */
    private Long positionId;
    /** 任职生效时间。 */
    private LocalDateTime effectiveStartTime;
    /** 任职失效时间，NULL 表示当前有效。 */
    private LocalDateTime effectiveEndTime;
    /** 状态：1-在职，2-已结束。 */
    private Integer status;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

