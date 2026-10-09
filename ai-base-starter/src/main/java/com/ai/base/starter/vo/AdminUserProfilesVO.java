package com.ai.base.starter.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 管理端批量用户查询请求参数。 */
@Getter
@Setter
public class AdminUserProfilesVO {
    /** 待查询的全局用户 ID 列表。 */
    @NotEmpty(message = "1201101")
    private List<@NotBlank(message = "1201101") String> userIds;
}

