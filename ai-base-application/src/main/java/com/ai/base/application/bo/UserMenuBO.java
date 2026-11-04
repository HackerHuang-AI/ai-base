package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 当前用户可见的导航菜单节点，不代表业务接口权限。 */
@Getter
@Setter
public class UserMenuBO {
    /** 当前空间内唯一的菜单编码，可作为前端导航节点标识。 */
    private String menuCode;
    /** 导航展示名称。 */
    private String resourceName;
    /** 菜单描述。 */
    private String desc;
    /** 资源类型：1-目录，2-页面。 */
    private Integer resourceType;
    /** 页面路由路径，目录为空。 */
    private String routePath;
    /** 前端图标标识。 */
    private String icon;
    /** 同级菜单升序排序值。 */
    private Integer sortNo;
    /** 可见子菜单，叶子节点返回空列表。 */
    private List<UserMenuBO> children;
}

