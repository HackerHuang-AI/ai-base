package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description: 持久化实体公共字段基类。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: BaseEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
public abstract class BaseEntity {
    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 创建时间。 */
    private LocalDateTime ctime;
    /** 更新时间。 */
    private LocalDateTime utime;
    /** 有效标识：1-有效，0-无效。 */
    @TableLogic(value = "1", delval = "0")
    private Integer valid;
    /** 乐观锁版本。 */
    @Version
    private Integer version;
}

