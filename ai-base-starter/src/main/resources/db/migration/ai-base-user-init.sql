CREATE DATABASE IF NOT EXISTS `ai_base` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_base`;

CREATE TABLE `tenant` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '租户 ID',
    `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
    `tenant_name` VARCHAR(128) NOT NULL COMMENT '租户名称',
    `tenant_type` TINYINT NOT NULL COMMENT '类型：1-个人空间，2-企业租户',
    `credit_code` VARCHAR(32) DEFAULT NULL COMMENT '统一社会信用代码，个人空间为空',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-停用',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    UNIQUE KEY `uk_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

CREATE TABLE `tenant_info` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户 ID',
    `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系人手机号',
    `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系人邮箱',
    `address` VARCHAR(256) DEFAULT NULL COMMENT '企业地址',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '租户简介',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户详情表';

CREATE TABLE `base_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `sso_user_id` VARCHAR(128) NOT NULL COMMENT 'SSO 用户全局唯一标识',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '账号名',
    `name` VARCHAR(64) NOT NULL COMMENT '用户姓名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-禁用',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sso_user_id` (`sso_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户表';

CREATE TABLE `user_info` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `mobile` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `job_number` VARCHAR(64) DEFAULT NULL COMMENT '工号',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户详情表';

CREATE TABLE `tenant_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '成员关系 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户 ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-已移除，3-待接受',
    `joined_at` DATETIME DEFAULT NULL COMMENT '加入时间',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人用户 ID',
    `updater_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人用户 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_user_status_valid` (`user_id`, `status`, `valid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员关系表';

CREATE TABLE `role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '所属租户 ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `scope_type` TINYINT NOT NULL COMMENT '适用范围：1-租户，2-组织，3-两者',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '角色说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，2-停用',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_code` (`tenant_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色表';

CREATE TABLE `role_permission` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限点 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE `user_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `tenant_user_id` BIGINT UNSIGNED NOT NULL COMMENT '租户成员关系 ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '授权生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '授权失效时间，NULL 表示当前有效',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_role` (`tenant_user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员角色关联表';

CREATE TABLE `organization_info` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '组织 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '所属租户 ID',
    `org_code` VARCHAR(64) NOT NULL COMMENT '组织编码',
    `org_name` VARCHAR(128) NOT NULL COMMENT '组织名称',
    `org_type` TINYINT NOT NULL DEFAULT 1 COMMENT '组织类型：1-部门，2-项目组，3-虚拟组织',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，2-停用',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人用户 ID',
    `updater_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人用户 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_org_code` (`tenant_id`, `org_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织信息表';

CREATE TABLE `organization_structure` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '组织架构 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '所属租户 ID',
    `structure_code` VARCHAR(64) NOT NULL COMMENT '组织架构编码',
    `structure_name` VARCHAR(128) NOT NULL COMMENT '组织架构名称',
    `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织架构：1-是，0-否',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，2-停用',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人用户 ID',
    `updater_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人用户 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_structure_code` (`tenant_id`, `structure_code`),
    KEY `idx_tenant_primary` (`tenant_id`, `is_primary`, `valid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构定义表';

CREATE TABLE `organization_structure_node` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '架构节点 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '所属租户 ID',
    `structure_id` BIGINT UNSIGNED NOT NULL COMMENT '组织架构 ID',
    `organization_id` BIGINT UNSIGNED NOT NULL COMMENT '组织信息 ID',
    `parent_node_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父架构节点 ID，NULL 表示唯一根节点',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级节点排序值，升序排列',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人用户 ID',
    `updater_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人用户 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_structure_organization` (`structure_id`, `organization_id`),
    KEY `idx_structure_parent_sort` (`structure_id`, `parent_node_id`, `sort_no`, `id`),
    KEY `idx_tenant_organization` (`tenant_id`, `organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构节点表';

CREATE TABLE `position` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '岗位 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '所属租户 ID',
    `position_code` VARCHAR(64) NOT NULL COMMENT '岗位编码',
    `position_name` VARCHAR(128) NOT NULL COMMENT '岗位名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '岗位说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，2-停用',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人用户 ID',
    `updater_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人用户 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_position_code` (`tenant_id`, `position_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

CREATE TABLE `organization_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '组织任职关系 ID',
    `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '所属租户 ID',
    `organization_id` BIGINT UNSIGNED NOT NULL COMMENT '组织信息 ID',
    `tenant_user_id` BIGINT UNSIGNED NOT NULL COMMENT '租户成员关系 ID',
    `position_id` BIGINT UNSIGNED NOT NULL COMMENT '岗位 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '任职生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '任职失效时间，NULL 表示当前有效',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-在职，2-已结束',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建人用户 ID',
    `updater_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后修改人用户 ID',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_organization_tenant_user_status` (`organization_id`, `tenant_user_id`, `status`, `valid`),
    KEY `idx_tenant_user_status` (`tenant_user_id`, `status`, `valid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织人员任职表';

CREATE TABLE `organization_user_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `organization_user_id` BIGINT UNSIGNED NOT NULL COMMENT '组织任职关系 ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '授权生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '授权失效时间，NULL 表示当前有效',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效，0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_organization_user_role` (`organization_user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织人员角色关联表';

