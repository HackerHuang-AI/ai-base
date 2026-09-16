CREATE DATABASE IF NOT EXISTS `ai_base` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_base`;

CREATE TABLE `permission` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '权限点 ID',
    `permission_code` VARCHAR(128) NOT NULL COMMENT '权限点编码',
    `permission_name` VARCHAR(128) NOT NULL COMMENT '权限点名称',
    `resource_type` VARCHAR(64) NOT NULL COMMENT '资源类型',
    `action` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '权限说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，2-停用',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台权限点表';

CREATE TABLE `platform_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '平台角色 ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '平台角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '平台角色名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '角色说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，2-停用',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台角色表';

CREATE TABLE `platform_role_permission` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `platform_role_id` BIGINT UNSIGNED NOT NULL COMMENT '平台角色 ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限点 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_role_permission` (`platform_role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台角色权限关联表';

CREATE TABLE `platform_user_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `platform_role_id` BIGINT UNSIGNED NOT NULL COMMENT '平台角色 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '授权生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '授权失效时间，NULL 表示当前有效',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_user_role` (`user_id`, `platform_role_id`),
    KEY `idx_platform_role_id` (`platform_role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户角色关联表';

