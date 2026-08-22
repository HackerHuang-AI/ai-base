package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private LocalDateTime ctime;
    private LocalDateTime utime;
    @TableLogic(value = "1", delval = "0")
    private Integer valid;
    @Version
    private Integer version;
}

