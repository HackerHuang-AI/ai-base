package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * 需使用乐观锁的业务实体基类。
 *
 * @author HUANGcong
 * @since 2026-10-09
 */
@Getter
@Setter
public abstract class VersionedEntity extends BaseEntity {
    /**
     * 乐观锁版本号。
     * 更新时 MyBatis-Plus 会以读取到的版本号作为更新条件，成功后递增该值。
     */
    @Version
    private Integer version;
}

