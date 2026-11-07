CREATE DATABASE IF NOT EXISTS `ai_base` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_base`;

# 用户表
CREATE TABLE `base_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '本地物理主键',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `mobile` VARCHAR(32) NOT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '账号名',
    `password_hash` VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt 密码哈希',
    `name` VARCHAR(64) DEFAULT NULL COMMENT '用户姓名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户表';

# 用户详情表
CREATE TABLE `user_info` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `job_number` VARCHAR(64) DEFAULT NULL COMMENT '工号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_user_info_user` FOREIGN KEY (`user_id`) REFERENCES `base_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户详情表';

# 用户身份标识表
CREATE TABLE `user_identity` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `identity_type` VARCHAR(32) NOT NULL COMMENT '身份类型',
    `identity_provider` VARCHAR(64) NOT NULL DEFAULT 'LOCAL' COMMENT '身份提供方',
    `identity_value` VARCHAR(256) NOT NULL COMMENT '身份标识值',
    `verified_at` DATETIME DEFAULT NULL COMMENT '认证通过时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_identity_type_provider_value` (`identity_type`, `identity_provider`, `identity_value`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_user_identity_user` FOREIGN KEY (`user_id`) REFERENCES `base_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户身份标识表';

# 登录审计表
CREATE TABLE `login_audit` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `user_id` VARCHAR(128) DEFAULT NULL COMMENT '全局用户 ID',
    `tenant_id` VARCHAR(128) DEFAULT NULL COMMENT '全局租户 ID',
    `identity_type` VARCHAR(32) NOT NULL COMMENT '身份类型',
    `identity_value_hash` CHAR(64) NOT NULL COMMENT '身份标识值 SHA-256 摘要',
    `login_result` TINYINT NOT NULL COMMENT '登录结果：1-成功 2-失败',
    `failure_code` VARCHAR(64) DEFAULT NULL COMMENT '失败码',
    `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '登录 IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    `login_at` DATETIME NOT NULL COMMENT '登录时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录审计表';

# 租户表
CREATE TABLE `tenant` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '本地物理主键',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '全局租户 ID',
    `tenant_name` VARCHAR(128) NOT NULL COMMENT '租户名称',
    `tenant_type` TINYINT NOT NULL COMMENT '类型：1-个人空间 2-企业租户',
    `credit_code` VARCHAR(32) DEFAULT NULL COMMENT '统一社会信用代码 个人空间为空',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`),
    UNIQUE KEY `uk_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

# 租户详情表
CREATE TABLE `tenant_info` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '全局租户 ID',
    `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系人手机号',
    `contact_email` VARCHAR(128) DEFAULT NULL COMMENT '联系人邮箱',
    `address` VARCHAR(256) DEFAULT NULL COMMENT '企业地址',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '租户简介',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`),
    CONSTRAINT `fk_tenant_info_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户详情表';

CREATE TABLE `tenant_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '成员关系 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '全局租户 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-已移除 3-待接受',
    `joined_at` DATETIME DEFAULT NULL COMMENT '加入时间',
    `creator_user_id` VARCHAR(128) NOT NULL COMMENT '创建人全局用户 ID',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_user_status_valid` (`user_id`, `status`, `valid`),
    CONSTRAINT `fk_tenant_user_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`),
    CONSTRAINT `fk_tenant_user_user` FOREIGN KEY (`user_id`) REFERENCES `base_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员关系表';

# 用户空间偏好表
CREATE TABLE `user_tenant_preference` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '本地物理主键',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `default_tenant_id` VARCHAR(128) DEFAULT NULL COMMENT '用户主动设置的默认空间 ID',
    `last_used_tenant_id` VARCHAR(128) DEFAULT NULL COMMENT '用户最近一次成功进入的空间 ID',
    `last_used_at` DATETIME(6) DEFAULT NULL COMMENT '最近进入空间的操作时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_default_tenant_id` (`default_tenant_id`),
    KEY `idx_last_used_tenant_id` (`last_used_tenant_id`),
    CONSTRAINT `fk_user_tenant_preference_user` FOREIGN KEY (`user_id`) REFERENCES `base_user` (`user_id`),
    CONSTRAINT `fk_user_tenant_preference_default_tenant` FOREIGN KEY (`default_tenant_id`) REFERENCES `tenant` (`tenant_id`),
    CONSTRAINT `fk_user_tenant_preference_last_used_tenant` FOREIGN KEY (`last_used_tenant_id`) REFERENCES `tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户空间偏好表';

# 企业租户前端资源表
CREATE TABLE `tenant_menu` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '租户前端资源 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '全局租户 ID',
    `menu_code` VARCHAR(128) NOT NULL COMMENT '菜单编码 同时作为接口鉴权标识',
    `resource_name` VARCHAR(128) NOT NULL COMMENT '前端资源名称',
    `desc` VARCHAR(256) DEFAULT NULL COMMENT '菜单描述',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型：1-目录 2-页面 3-按钮',
    `route_path` VARCHAR(256) DEFAULT NULL COMMENT '前端路由路径 仅页面资源使用',
    `parent_tenant_menu_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父租户前端资源 ID NULL 表示根资源',
    `men_path` VARCHAR(512) NOT NULL COMMENT '菜单编码路径 使用分号分隔',
    `menu_level` TINYINT UNSIGNED NOT NULL COMMENT '资源层级：根目录为 0 依次递增',
    `icon` VARCHAR(128) DEFAULT NULL COMMENT '前端图标标识',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级资源排序值 升序排列',
    `deletable` TINYINT NOT NULL DEFAULT 0 COMMENT '租户是否允许删除：1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_menu_id` (`tenant_id`, `id`),
    UNIQUE KEY `uk_tenant_menu_code` (`tenant_id`, `menu_code`),
    KEY `idx_tenant_parent_sort` (`tenant_id`, `parent_tenant_menu_id`, `sort_no`, `id`),
    CONSTRAINT `fk_tenant_menu_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`),
    CONSTRAINT `fk_tenant_menu_parent` FOREIGN KEY (`tenant_id`, `parent_tenant_menu_id`) REFERENCES `tenant_menu` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业租户前端资源表';

# 个人空间共享前端菜单 由运营端通过 Dubbo 统一管理
CREATE TABLE `personal_tenant_menu` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '个人空间共享菜单 ID',
    `menu_code` VARCHAR(128) NOT NULL COMMENT '菜单编码 同时作为接口鉴权标识',
    `resource_name` VARCHAR(128) NOT NULL COMMENT '前端资源名称',
    `desc` VARCHAR(256) DEFAULT NULL COMMENT '菜单描述',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型：1-目录 2-页面 3-按钮',
    `route_path` VARCHAR(256) DEFAULT NULL COMMENT '前端路由路径 仅页面资源使用',
    `parent_personal_menu_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父个人菜单 ID NULL 表示根资源',
    `men_path` VARCHAR(512) NOT NULL COMMENT '菜单编码路径 使用分号分隔',
    `menu_level` TINYINT UNSIGNED NOT NULL COMMENT '资源层级：根目录为 0 依次递增',
    `icon` VARCHAR(128) DEFAULT NULL COMMENT '前端图标标识',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级资源排序值 升序排列',
    `deletable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许运营端删除：1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人运营用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人运营用户 ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_personal_tenant_menu_code` (`menu_code`),
    KEY `idx_personal_tenant_parent_sort` (`parent_personal_menu_id`, `sort_no`, `id`),
    CONSTRAINT `fk_personal_menu_parent` FOREIGN KEY (`parent_personal_menu_id`) REFERENCES `personal_tenant_menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人空间共享前端菜单表';

CREATE TABLE `role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '所属全局租户 ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '角色说明',
    `deletable` TINYINT NOT NULL DEFAULT 1 COMMENT '租户是否允许删除：1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_id` (`tenant_id`, `id`),
    UNIQUE KEY `uk_tenant_role_code` (`tenant_id`, `role_code`),
    CONSTRAINT `fk_role_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色表';

# 角色前端资源关联表 由当前租户管理员维护
CREATE TABLE `role_menu` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '全局租户 ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `tenant_menu_id` BIGINT UNSIGNED NOT NULL COMMENT '租户前端资源 ID',
    `include_descendants` TINYINT NOT NULL DEFAULT 0 COMMENT '是否继承子资源权限：1-是 0-否',
    `deletable` TINYINT NOT NULL DEFAULT 1 COMMENT '该角色菜单关联是否允许删除：1-允许 0-不允许',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_menu_valid` (`tenant_id`, `role_id`, `tenant_menu_id`, `valid`),
    KEY `idx_tenant_menu_id` (`tenant_id`, `tenant_menu_id`),
    CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`tenant_id`, `role_id`) REFERENCES `role` (`tenant_id`, `id`),
    CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`tenant_id`, `tenant_menu_id`) REFERENCES `tenant_menu` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色前端资源关联表';

CREATE TABLE `user_role` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '全局租户 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '授权生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '授权失效时间 NULL 表示当前有效',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_role_valid` (`tenant_id`, `user_id`, `role_id`, `valid`),
    KEY `idx_role_id` (`tenant_id`, `role_id`),
    CONSTRAINT `fk_user_role_member` FOREIGN KEY (`tenant_id`, `user_id`) REFERENCES `tenant_user` (`tenant_id`, `user_id`),
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`tenant_id`, `role_id`) REFERENCES `role` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员角色关联表';

CREATE TABLE `organization_info` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '组织 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '所属全局租户 ID',
    `org_code` VARCHAR(64) NOT NULL COMMENT '组织编码',
    `org_name` VARCHAR(128) NOT NULL COMMENT '组织名称',
    `org_type` TINYINT NOT NULL DEFAULT 1 COMMENT '组织类型：1-部门 2-项目组 3-虚拟组织',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) NOT NULL COMMENT '创建人全局用户 ID',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_organization_id` (`tenant_id`, `id`),
    UNIQUE KEY `uk_tenant_org_code` (`tenant_id`, `org_code`),
    CONSTRAINT `fk_organization_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织信息表';

CREATE TABLE `organization_structure` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '组织架构 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '所属全局租户 ID',
    `structure_code` VARCHAR(64) NOT NULL COMMENT '组织架构编码',
    `structure_name` VARCHAR(128) NOT NULL COMMENT '组织架构名称',
    `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主组织架构：1-是 0-否',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) NOT NULL COMMENT '创建人全局用户 ID',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `primary_tenant_id` VARCHAR(128) GENERATED ALWAYS AS (CASE WHEN `is_primary` = 1 AND `status` = 1 AND `valid` = 1 THEN `tenant_id` ELSE NULL END) STORED COMMENT '启用且有效的主架构租户唯一键',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_structure_id` (`tenant_id`, `id`),
    UNIQUE KEY `uk_tenant_structure_code` (`tenant_id`, `structure_code`),
    UNIQUE KEY `uk_tenant_primary_structure` (`primary_tenant_id`),
    KEY `idx_tenant_primary` (`tenant_id`, `is_primary`, `valid`),
    CONSTRAINT `fk_organization_structure_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构定义表';

CREATE TABLE `organization_structure_node` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '架构节点 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '所属全局租户 ID',
    `structure_id` BIGINT UNSIGNED NOT NULL COMMENT '组织架构 ID',
    `organization_id` BIGINT UNSIGNED NOT NULL COMMENT '组织信息 ID',
    `parent_node_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父架构节点 ID NULL 表示唯一根节点',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级节点排序值 升序排列',
    `root_structure_id` BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN `parent_node_id` IS NULL AND `valid` = 1 THEN `structure_id` ELSE NULL END) STORED COMMENT '有效根节点架构唯一键',
    `creator_user_id` VARCHAR(128) NOT NULL COMMENT '创建人全局用户 ID',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_structure_node_id` (`tenant_id`, `structure_id`, `id`),
    UNIQUE KEY `uk_structure_organization` (`tenant_id`, `structure_id`, `organization_id`),
    UNIQUE KEY `uk_structure_single_root` (`tenant_id`, `root_structure_id`),
    KEY `idx_structure_parent_sort` (`tenant_id`, `structure_id`, `parent_node_id`, `sort_no`, `id`),
    KEY `idx_tenant_organization` (`tenant_id`, `organization_id`),
    CONSTRAINT `fk_structure_node_structure` FOREIGN KEY (`tenant_id`, `structure_id`) REFERENCES `organization_structure` (`tenant_id`, `id`),
    CONSTRAINT `fk_structure_node_organization` FOREIGN KEY (`tenant_id`, `organization_id`) REFERENCES `organization_info` (`tenant_id`, `id`),
    CONSTRAINT `fk_structure_node_parent` FOREIGN KEY (`tenant_id`, `structure_id`, `parent_node_id`) REFERENCES `organization_structure_node` (`tenant_id`, `structure_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构节点表';

CREATE TABLE `position` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '岗位 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '所属全局租户 ID',
    `position_code` VARCHAR(64) NOT NULL COMMENT '岗位编码',
    `position_name` VARCHAR(128) NOT NULL COMMENT '岗位名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '岗位说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用 2-停用',
    `creator_user_id` VARCHAR(128) NOT NULL COMMENT '创建人全局用户 ID',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_position_id` (`tenant_id`, `id`),
    UNIQUE KEY `uk_tenant_position_code` (`tenant_id`, `position_code`),
    CONSTRAINT `fk_position_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

CREATE TABLE `organization_user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '组织任职关系 ID',
    `tenant_id` VARCHAR(128) NOT NULL COMMENT '所属全局租户 ID',
    `organization_id` BIGINT UNSIGNED NOT NULL COMMENT '组织信息 ID',
    `user_id` VARCHAR(128) NOT NULL COMMENT '全局用户 ID',
    `position_id` BIGINT UNSIGNED NOT NULL COMMENT '岗位 ID',
    `effective_start_time` DATETIME NOT NULL COMMENT '任职生效时间',
    `effective_end_time` DATETIME DEFAULT NULL COMMENT '任职失效时间 NULL 表示当前有效',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-在职 2-已结束',
    `creator_user_id` VARCHAR(128) NOT NULL COMMENT '创建人全局用户 ID',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人全局用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识：1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    KEY `idx_organization_user_status` (`tenant_id`, `organization_id`, `user_id`, `status`, `valid`),
    KEY `idx_tenant_user_status` (`tenant_id`, `user_id`, `status`, `valid`),
    KEY `idx_tenant_position` (`tenant_id`, `position_id`),
    CONSTRAINT `fk_organization_user_member` FOREIGN KEY (`tenant_id`, `user_id`) REFERENCES `tenant_user` (`tenant_id`, `user_id`),
    CONSTRAINT `fk_organization_user_organization` FOREIGN KEY (`tenant_id`, `organization_id`) REFERENCES `organization_info` (`tenant_id`, `id`),
    CONSTRAINT `fk_organization_user_position` FOREIGN KEY (`tenant_id`, `position_id`) REFERENCES `position` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织人员任职表';
