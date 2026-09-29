package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * @Description: 全局用户实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: UserEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("base_user")
public class UserEntity extends BaseEntity {
    /** 全局用户 ID，跨库分表关联使用。 */
    private String userId;
    /** 手机号。 */
    private String mobile;
    /** 邮箱。 */
    private String email;
    /** 账号名。 */
    private String username;
    /** BCrypt 密码哈希。 */
    private String passwordHash;
    /** 用户姓名。 */
    private String name;
    /** 状态：1-正常，2-禁用。 */
    private Integer status;
}

