package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 租户角色实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: RoleEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("role")
public class RoleEntity extends BaseEntity {
    /** 所属全局租户 ID。 */
    private String tenantId;
    /** 角色编码。 */
    private String roleCode;
    /** 角色名称。 */
    private String roleName;
    /** 适用范围：1-租户，2-组织，3-两者。 */
    private Integer scopeType;
    /** 角色说明。 */
    private String description;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
}

