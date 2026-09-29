package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 平台角色实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: PlatformRoleEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("platform_role")
public class PlatformRoleEntity extends BaseEntity {
    /** 平台角色编码。 */
    private String roleCode;
    /** 平台角色名称。 */
    private String roleName;
    /** 角色说明。 */
    private String description;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
}

