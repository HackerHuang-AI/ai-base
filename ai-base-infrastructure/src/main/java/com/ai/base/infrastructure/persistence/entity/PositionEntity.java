package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("position")
public class PositionEntity extends BaseEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 岗位编码。 */
    private String positionCode;
    /** 岗位名称。 */
    private String positionName;
    /** 岗位说明。 */
    private String description;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
    /** 创建人全局用户 ID。 */
    private String creatorUserId;
    /** 最后修改人全局用户 ID。 */
    private String updaterUserId;
}

