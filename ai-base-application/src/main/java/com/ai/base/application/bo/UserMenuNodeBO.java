package com.ai.base.application.bo;

import lombok.Data;

/**
 * 用户菜单扁平节点（menu 关联查询结果行），由应用层负责组装成树。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Data
public class UserMenuNodeBO {
    private Long id;
    private Long parentId;
    private String menuCode;
    private String resourceName;
    private String desc;
    private Integer resourceType;
    private String routePath;
    private String icon;
    private Integer sortNo;
    private boolean authorized;
}
