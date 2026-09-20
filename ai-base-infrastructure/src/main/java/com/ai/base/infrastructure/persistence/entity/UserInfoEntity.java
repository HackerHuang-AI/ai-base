package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_info")
public class UserInfoEntity extends BaseEntity {
    /** 全局用户 ID。 */
    private String userId;
    /** 头像地址。 */
    private String avatarUrl;
    /** 工号。 */
    private String jobNumber;
}

