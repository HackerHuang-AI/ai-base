package com.ai.base.application.bo;

import lombok.Data;

/**
 * 用户账号（user 表语义字段）。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Data
public class UserAccountBO {
    private String userId;
    private String mobile;
    private String email;
    private String username;
    private String passwordHash;
    private String name;
    private Integer status;
}
