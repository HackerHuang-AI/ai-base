package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_tenant_preference")
public class UserTenantPreferenceEntity extends VersionedEntity {
    /** 全局用户 ID。 */
    private String userId;
    /** 用户主动设置的默认空间 ID，NULL 表示未设置。 */
    private String defaultTenantId;
    /** 用户最近一次成功进入的空间 ID。 */
    private String lastUsedTenantId;
    /** 最近进入空间的操作时间，用于防止较旧记录覆盖较新记录。 */
    private LocalDateTime lastUsedAt;
}

