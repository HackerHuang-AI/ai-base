package com.ai.base.application.bo;

import lombok.Data;

/**
 * 用户扩展信息（user_info 表语义字段）。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Data
public class UserInfoBO {
    private String userId;
    private String avatarUrl;
    private String jobNumber;
}
