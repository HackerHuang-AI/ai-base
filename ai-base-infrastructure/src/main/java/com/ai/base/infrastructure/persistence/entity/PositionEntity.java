package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("position")
public class PositionEntity extends BaseEntity {
    /** 所属租户 ID。 */
    private Long tenantId;
    /** 岗位编码。 */
    private String positionCode;
    /** 岗位名称。 */
    private String positionName;
    /** 岗位说明。 */
    private String description;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
    /** 创建人用户 ID。 */
    private Long creatorUserId;
    /** 最后修改人用户 ID。 */
    private Long updaterUserId;
}

