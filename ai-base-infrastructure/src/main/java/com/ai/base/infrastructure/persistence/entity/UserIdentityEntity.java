package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户身份认证实体。
 *
 * @author HUANGcong
 * @since 2026-09-24
 */
@Getter
@Setter
@TableName("user_identity")
public class UserIdentityEntity extends VersionedEntity {
    /** 全局用户 ID。 */
    private String userId;
    /** 身份类型。 */
    private String identityType;
    /** 身份提供方。 */
    private String identityProvider;
    /** 身份标识值。 */
    private String identityValue;
    /** 认证通过时间。 */
    private LocalDateTime verifiedAt;
}

