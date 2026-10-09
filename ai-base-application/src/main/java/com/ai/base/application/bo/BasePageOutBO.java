package com.ai.base.application.bo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Service 分页返回结果。 */
@Getter
@Setter
public class BasePageOutBO<T> {
    /** 当前页记录。 */
    private List<T> records;
    /** 符合筛选条件的总记录数。 */
    private long total;
    /** 当前页码，从 1 开始。 */
    private long pageNo;
    /** 每页记录数。 */
    private long pageSize;
}
