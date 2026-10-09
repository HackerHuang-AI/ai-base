package com.ai.base.application.service;

import com.ai.base.application.bo.*;

import java.util.List;

/** 用户资料服务。 */
public interface UserService {
    PersonalUserProfileBO getCurrentProfile();

    void updateCurrentProfile(PersonalUserUpdateBO update);

    void updateCurrentUsername(PersonalUsernameUpdateBO update);

    void updateCurrentMobile(PersonalMobileUpdateBO update);

    void updateCurrentPersonalTenant(PersonalTenantUpdateBO update);

    List<UserProfileBO> listProfiles(List<String> userIds);

    List<UserWithTenantsBO> listProfilesWithTenants(List<String> userIds);

    BasePageOutBO<UserProfileBO> adminUserPage(AdminUserQueryBO query);
}

