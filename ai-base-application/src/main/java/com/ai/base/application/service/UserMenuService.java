package com.ai.base.application.service;

import com.ai.base.application.bo.UserMenuBO;

import java.util.List;

public interface UserMenuService {
    /** 查询可信登录上下文中当前用户、当前空间的可见导航树。 */
    List<UserMenuBO> treeCurrent();
}

