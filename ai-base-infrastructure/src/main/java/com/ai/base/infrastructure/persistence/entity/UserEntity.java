package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("base_user")
public class UserEntity extends BaseEntity {
    /** 全局用户 ID，跨库分表关联使用。 */
    private String userId;
    /** 手机号。 */
    private String mobile;
    /** 邮箱。 */
    private String email;
    /** 账号名。 */
    private String username;
    /** 用户姓名。 */
    private String name;
    /** 状态：1-正常，2-禁用。 */
    private Integer status;
}

