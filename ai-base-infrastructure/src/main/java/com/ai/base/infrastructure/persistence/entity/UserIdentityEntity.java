package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description: 用户身份认证实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: UserIdentityEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("user_identity")
public class UserIdentityEntity extends BaseEntity {
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

