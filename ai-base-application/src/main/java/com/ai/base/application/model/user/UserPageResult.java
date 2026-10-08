package com.ai.base.application.model.user;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class UserPageResult {
    /** 当前页用户信息。 */
    private List<UserProfile> records;
    /** 符合筛选条件的总记录数。 */
    private long total;
    /** 当前页码，从 1 开始。 */
    private long pageNo;
    /** 每页条数。 */
    private long pageSize;
}

