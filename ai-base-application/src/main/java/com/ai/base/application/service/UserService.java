package com.ai.base.application.service;

import com.ai.base.application.model.user.UserPageResult;
import com.ai.base.application.model.user.UserProfile;
import com.ai.base.application.model.user.UserQuery;

/**
 * 用户资料查询服务接口。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
public interface UserService {
    UserProfile getProfile(UserQuery query);

    UserPageResult page(UserQuery query);
}

