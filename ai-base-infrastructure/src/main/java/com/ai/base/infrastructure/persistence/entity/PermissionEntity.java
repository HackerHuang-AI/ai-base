package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("permission")
public class PermissionEntity extends BaseEntity {
    /** 权限点编码。 */
    private String permissionCode;
    /** 权限点名称。 */
    private String permissionName;
    /** 资源类型。 */
    private String resourceType;
    /** 操作类型。 */
    private String action;
    /** 权限说明。 */
    private String description;
    /** 状态：1-启用，2-停用。 */
    private Integer status;
}

