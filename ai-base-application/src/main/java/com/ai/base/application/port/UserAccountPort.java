package com.ai.base.application.port;

import com.ai.base.application.bo.UserAccountBO;
import com.ai.base.application.bo.UserInfoBO;
import com.ai.base.application.bo.UserIdentityBO;

import java.util.Optional;

/**
 * 用户账号存取端口：覆盖 user / user_info / user_identity 三张表的业务语义访问。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface UserAccountPort {

    /** 按用户标识查询账号，不校验状态。 */
    Optional<UserAccountBO> findUser(String userId);

    /** 按用户标识查询生效（status=1）账号。 */
    Optional<UserAccountBO> findActiveUser(String userId);

    /** 更新账号字段，返回 false 表示版本冲突或记录不存在。 */
    boolean updateAccount(UserAccountBO account);

    /** 查询用户扩展信息。 */
    Optional<UserInfoBO> findUserInfo(String userId);

    /** 保存用户扩展信息：不存在则插入，已存在则更新，失败抛出数据版本冲突。 */
    void upsertUserInfo(UserInfoBO info);

    /** 按身份三元组查询身份凭证。 */
    Optional<UserIdentityBO> findIdentity(String identityType, String identityProvider, String identityValue);

    /** 按用户与身份类型/提供方查询身份凭证。 */
    Optional<UserIdentityBO> findUserIdentity(String userId, String identityType, String identityProvider);

    /** 保存身份凭证：不存在则插入，已存在则更新值，失败抛出数据版本冲突。 */
    void upsertIdentity(String userId, String identityType, String identityProvider, String identityValue);
}
