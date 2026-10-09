package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 管理端用户及其所属租户信息。 */
@Getter
@Setter
public class UserWithTenantsBO extends UserProfileBO {
    /** 用户所属的全部有效租户。 */
    private List<UserTenantBO> tenants;
}

