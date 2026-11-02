package com.ai.base.infrastructure.persistence.dto;

import lombok.Getter;
import lombok.Setter;

/** 当前空间导航菜单查询结果行。 */
@Getter
@Setter
public class UserMenuRow {
    /** 菜单内部主键，仅用于组装树。 */
    private Long id;
    /** 父菜单内部主键，根节点为空。 */
    private Long parentId;
    /** 当前空间内唯一的菜单编码。 */
    private String menuCode;
    /** 导航展示名称。 */
    private String resourceName;
    /** 资源类型：1-目录，2-页面。 */
    private Integer resourceType;
    /** 页面路由路径。 */
    private String routePath;
    /** 前端图标标识。 */
    private String icon;
    /** 同级菜单升序排序值。 */
    private Integer sortNo;
    /** 当前用户是否具有该菜单的展示授权。 */
    private boolean authorized;
}

