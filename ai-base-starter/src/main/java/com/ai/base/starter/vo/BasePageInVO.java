package com.ai.base.starter.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * author： wb_huangcong03
 * date： 2026/10/8
 * description：
 */
@Getter
@Setter
public class BasePageInVO {

    @Min(value = 1, message = "1201102")
    private long pageNo = 1;
    @Min(value = 1, message = "1201103")
    @Max(value = 100, message = "1201103")
    private long pageSize = 20;
}
