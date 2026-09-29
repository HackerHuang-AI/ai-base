package com.ai.base.application.service;

import com.ai.base.application.model.user.UserPageResult;
import com.ai.base.application.model.user.UserProfile;
import com.ai.base.application.model.user.UserQuery;

/**
 * @Description: 用户资料查询服务接口。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.application.service
 * @ClassName: UserService
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
public interface UserService {
    UserProfile getProfile(UserQuery query);

    UserPageResult page(UserQuery query);
}

