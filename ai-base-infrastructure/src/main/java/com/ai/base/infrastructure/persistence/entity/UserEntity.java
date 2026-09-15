package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("base_user")
public class UserEntity extends BaseEntity {
    /** SSO 用户全局唯一标识。 */
    private String ssoUserId;
    /** 账号名。 */
    private String username;
    /** 用户姓名。 */
    private String name;
    /** 状态：1-正常，2-禁用。 */
    private Integer status;
}

