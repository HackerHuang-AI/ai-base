package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

/** 当前个人用户资料。 */
@Getter
@Setter
public class PersonalUserProfileBO extends UserProfileBO {
    /** 当前用户的个人空间。 */
    private UserTenantBO personalTenant;
}

