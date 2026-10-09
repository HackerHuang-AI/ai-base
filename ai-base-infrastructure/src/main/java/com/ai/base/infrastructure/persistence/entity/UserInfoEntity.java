package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户扩展信息实体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
@TableName("user_info")
public class UserInfoEntity extends VersionedEntity {
    /** 全局用户 ID。 */
    private String userId;
    /** 头像地址。 */
    private String avatarUrl;
    /** 工号。 */
    private String jobNumber;
}

