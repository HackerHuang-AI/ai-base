CREATE DATABASE IF NOT EXISTS `ai_base` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_base`;

# 用户端前端资源模板表 系统级 由运营端维护 租户入驻时按模板初始化
CREATE TABLE `menu_template` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '前端资源模板 ID',
    `menu_code` VARCHAR(128) NOT NULL COMMENT '菜单编码 同时作为接口鉴权标识',
    `resource_name` VARCHAR(128) NOT NULL COMMENT '前端资源名称',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型 1-目录 2-页面 3-按钮',
    `route_path` VARCHAR(256) DEFAULT NULL COMMENT '前端路由路径 仅页面资源使用',
    `parent_menu_template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父前端资源模板 ID NULL 表示根资源',
    `men_path` VARCHAR(512) NOT NULL COMMENT '菜单编码路径 使用分号分隔',
    `menu_level` TINYINT UNSIGNED NOT NULL COMMENT '资源层级 根目录为 0 依次递增',
    `icon` VARCHAR(128) DEFAULT NULL COMMENT '前端图标标识',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '同级资源排序值 升序排列',
    `deletable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许运营端删除 1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人运营用户 ID 系统初始化为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人运营用户 ID 系统初始化为空',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识 1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    KEY `idx_parent_template_sort` (`parent_menu_template_id`, `sort_no`, `id`),
    CONSTRAINT `fk_menu_template_parent` FOREIGN KEY (`parent_menu_template_id`) REFERENCES `menu_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户端前端资源模板表';

# 根目录及用户端菜单初始化
INSERT INTO `menu_template` (
    `id`, `menu_code`, `resource_name`, `resource_type`, `route_path`, `parent_menu_template_id`, `men_path`, `menu_level`, `sort_no`, `deletable`, `status`, `valid`, `version`
) VALUES
(1, 'ROOT', '根目录', 1, NULL, NULL, 'ROOT', 0, 0, 0, 1, 1, 0),
(2, 'HOME', '首页', 2, '/home', 1, 'ROOT;HOME', 1, 1, 0, 1, 1, 0),
(3, 'HOME_PROFILE', '个人中心', 2, '/profile', 2, 'ROOT;HOME;HOME_PROFILE', 2, 1, 0, 1, 1, 0),
(6, 'AI', 'AI智库', 1, NULL, 1, 'ROOT;AI', 1, 2, 0, 1, 1, 0),
(7, 'TREASURY', '司库管理', 1, NULL, 1, 'ROOT;TREASURY', 1, 3, 0, 1, 1, 0),
(8, 'DB', '数据库', 1, NULL, 1, 'ROOT;DB', 1, 4, 0, 1, 1, 0),
(9, 'DOCS', '企业文库', 1, NULL, 1, 'ROOT;DOCS', 1, 5, 0, 1, 1, 0),
(10, 'MONITOR', '监控中心', 1, NULL, 1, 'ROOT;MONITOR', 1, 6, 0, 1, 1, 0),
(11, 'AI_LLM', '大模型', 1, NULL, 6, 'ROOT;AI;AI_LLM', 2, 1, 0, 1, 1, 0),
(12, 'AI_AGENT', 'Agent', 1, NULL, 6, 'ROOT;AI;AI_AGENT', 2, 2, 0, 1, 1, 0),
(13, 'AI_CAPABILITY', '基础能力', 1, NULL, 6, 'ROOT;AI;AI_CAPABILITY', 2, 3, 0, 1, 1, 0),
(14, 'AI_APP', '智能应用', 1, NULL, 6, 'ROOT;AI;AI_APP', 2, 4, 0, 1, 1, 0),
(15, 'AI_SOLUTION', '解决方案', 1, NULL, 6, 'ROOT;AI;AI_SOLUTION', 2, 5, 0, 1, 1, 0),
(16, 'AI_MARKET', '模型市场', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_MARKET', 3, 1, 0, 1, 1, 0),
(17, 'AI_MODEL_INFO', '模型信息', 2, '/ai/models/info', 16, 'ROOT;AI;AI_LLM;AI_MARKET;AI_MODEL_INFO', 4, 1, 0, 1, 1, 0),
(18, 'AI_MODEL_COMPARE', '模型对比', 2, '/ai/models/compare', 16, 'ROOT;AI;AI_LLM;AI_MARKET;AI_MODEL_COMPARE', 4, 2, 0, 1, 1, 0),
(19, 'AI_MODEL_LIST', '模型清单', 2, '/ai/models/list', 16, 'ROOT;AI;AI_LLM;AI_MARKET;AI_MODEL_LIST', 4, 3, 0, 1, 1, 0),
(20, 'AI_LIBRARY', '模型库', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_LIBRARY', 3, 2, 0, 1, 1, 0),
(21, 'AI_MODEL_TUNE', '模型微调', 2, '/ai/library/tuning', 20, 'ROOT;AI;AI_LLM;AI_LIBRARY;AI_MODEL_TUNE', 4, 1, 0, 1, 1, 0),
(22, 'AI_MODEL_COMPRESS', '模型压缩', 2, '/ai/library/compression', 20, 'ROOT;AI;AI_LLM;AI_LIBRARY;AI_MODEL_COMPRESS', 4, 2, 0, 1, 1, 0),
(23, 'AI_MODEL_DEPLOY', '模型部署', 2, '/ai/library/deployments', 20, 'ROOT;AI;AI_LLM;AI_LIBRARY;AI_MODEL_DEPLOY', 4, 3, 0, 1, 1, 0),
(24, 'AI_MODEL_MANAGE', '模型管理', 2, '/ai/library/models', 20, 'ROOT;AI;AI_LLM;AI_LIBRARY;AI_MODEL_MANAGE', 4, 4, 0, 1, 1, 0),
(25, 'AI_RESOURCE', '资源中心', 2, '/ai/library/resources', 20, 'ROOT;AI;AI_LLM;AI_LIBRARY;AI_RESOURCE', 4, 5, 0, 1, 1, 0),
(26, 'AI_EVAL', '评测工具', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_EVAL', 3, 3, 0, 1, 1, 0),
(27, 'AI_MODEL_SET', '模型集', 2, '/ai/evaluation/model-sets', 26, 'ROOT;AI;AI_LLM;AI_EVAL;AI_MODEL_SET', 4, 1, 0, 1, 1, 0),
(28, 'AI_COMPARE_EVAL', '对比评测', 2, '/ai/evaluation/compare', 26, 'ROOT;AI;AI_LLM;AI_EVAL;AI_COMPARE_EVAL', 4, 2, 0, 1, 1, 0),
(29, 'AI_MODEL_EVAL', '模型评测', 2, '/ai/evaluation/models', 26, 'ROOT;AI;AI_LLM;AI_EVAL;AI_MODEL_EVAL', 4, 3, 0, 1, 1, 0),
(30, 'AI_PROMPT', '提示词工厂', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_PROMPT', 3, 4, 0, 1, 1, 0),
(31, 'AI_PROMPT_MARKET', '提示词市场', 2, '/ai/prompts/market', 30, 'ROOT;AI;AI_LLM;AI_PROMPT;AI_PROMPT_MARKET', 4, 1, 0, 1, 1, 0),
(32, 'AI_PROMPT_OPTIMIZE', '提示词优化', 2, '/ai/prompts/optimize', 30, 'ROOT;AI;AI_LLM;AI_PROMPT;AI_PROMPT_OPTIMIZE', 4, 2, 0, 1, 1, 0),
(33, 'AGENT_ECO', 'Agent生态', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_ECO', 3, 1, 0, 1, 1, 0),
(34, 'AGENT_MARKET', 'Agent广场', 2, '/agent/ecosystem/market', 33, 'ROOT;AI;AI_AGENT;AGENT_ECO;AGENT_MARKET', 4, 1, 0, 1, 1, 0),
(35, 'AGENT_REGISTER', 'Agent注册', 2, '/agent/ecosystem/register', 33, 'ROOT;AI;AI_AGENT;AGENT_ECO;AGENT_REGISTER', 4, 2, 0, 1, 1, 0),
(36, 'AGENT_DASHBOARD', 'Agent管理大盘', 2, '/agent/ecosystem/dashboard', 33, 'ROOT;AI;AI_AGENT;AGENT_ECO;AGENT_DASHBOARD', 4, 3, 0, 1, 1, 0),
(37, 'AGENT_DEV', 'Agent开发', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_DEV', 3, 2, 0, 1, 1, 0),
(38, 'AGENT_CONFIG', 'Agent配置', 2, '/agent/development/config', 37, 'ROOT;AI;AI_AGENT;AGENT_DEV;AGENT_CONFIG', 4, 1, 0, 1, 1, 0),
(39, 'AGENT_WORKFLOW', '工作流', 2, '/agent/development/workflows', 37, 'ROOT;AI;AI_AGENT;AGENT_DEV;AGENT_WORKFLOW', 4, 2, 0, 1, 1, 0),
(40, 'AGENT_TOOL', '工具', 2, '/agent/development/tools', 37, 'ROOT;AI;AI_AGENT;AGENT_DEV;AGENT_TOOL', 4, 3, 0, 1, 1, 0),
(41, 'AGENT_EVAL', 'Agent评测', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_EVAL', 3, 3, 0, 1, 1, 0),
(42, 'AGENT_EVAL_TASK', '评测任务', 2, '/agent/evaluation/tasks', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_EVAL_TASK', 4, 1, 0, 1, 1, 0),
(43, 'AGENT_EVAL_METRIC', '评测指标', 2, '/agent/evaluation/metrics', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_EVAL_METRIC', 4, 2, 0, 1, 1, 0),
(44, 'AGENT_EVAL_SET', '评测集', 2, '/agent/evaluation/sets', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_EVAL_SET', 4, 3, 0, 1, 1, 0),
(45, 'AGENT_TEST_OBJECT', '测试对象', 2, '/agent/evaluation/objects', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_TEST_OBJECT', 4, 4, 0, 1, 1, 0),
(46, 'AGENT_OBSERVE', 'Agent监控', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE', 3, 4, 0, 1, 1, 0),
(47, 'AGENT_TRACE', 'Trace查询', 2, '/agent/observability/traces', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_TRACE', 4, 1, 0, 1, 1, 0),
(48, 'AGENT_TRACE_MANAGE', 'Trace管理', 2, '/agent/observability/trace-management', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_TRACE_MANAGE', 4, 2, 0, 1, 1, 0),
(49, 'AGENT_STAT', '指标监控', 2, '/agent/observability/metrics', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_STAT', 4, 3, 0, 1, 1, 0),
(50, 'AGENT_AUTO_TASK', '自动化任务', 2, '/agent/observability/automation', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_AUTO_TASK', 4, 4, 0, 1, 1, 0),
(51, 'KNOWLEDGE_BASE', '知识库', 1, NULL, 6, 'ROOT;AI;KNOWLEDGE_BASE', 2, 6, 0, 1, 1, 0),
(52, 'KNOWLEDGE_MANAGE', '知识管理', 2, '/knowledge/management', 51, 'ROOT;AI;KNOWLEDGE_BASE;KNOWLEDGE_MANAGE', 3, 1, 0, 1, 1, 0),
(53, 'KNOWLEDGE_SEARCH', '检索测试', 2, '/knowledge/search-test', 51, 'ROOT;AI;KNOWLEDGE_BASE;KNOWLEDGE_SEARCH', 3, 2, 0, 1, 1, 0),
(54, 'KNOWLEDGE_LIB', '知识库列表', 2, '/knowledge/libraries', 51, 'ROOT;AI;KNOWLEDGE_BASE;KNOWLEDGE_LIB', 3, 3, 0, 1, 1, 0),
(55, 'DATA_AGENT_KB', 'DataAgent知识库', 2, '/knowledge/data-agent', 51, 'ROOT;AI;KNOWLEDGE_BASE;DATA_AGENT_KB', 3, 4, 0, 1, 1, 0),
(56, 'MCP_MARKET', 'MCP广场', 1, NULL, 6, 'ROOT;AI;MCP_MARKET', 2, 7, 0, 1, 1, 0),
(57, 'MCP_DEBUG', 'MCP调试', 2, '/mcp/debug', 56, 'ROOT;AI;MCP_MARKET;MCP_DEBUG', 3, 1, 0, 1, 1, 0),
(58, 'TOOL_POOL', 'Tool池', 2, '/mcp/tool-pool', 56, 'ROOT;AI;MCP_MARKET;TOOL_POOL', 3, 2, 0, 1, 1, 0),
(59, 'MCP_PLAYGROUND', 'MCP Playground', 2, '/mcp/playground', 56, 'ROOT;AI;MCP_MARKET;MCP_PLAYGROUND', 3, 3, 0, 1, 1, 0),
(60, 'MEMORY', '记忆', 1, NULL, 6, 'ROOT;AI;MEMORY', 2, 8, 0, 1, 1, 0),
(61, 'MEMORY_MANAGE', '记忆管理', 2, '/memory/management', 60, 'ROOT;AI;MEMORY;MEMORY_MANAGE', 3, 1, 0, 1, 1, 0),
(62, 'SKILLS', 'Skills', 1, NULL, 6, 'ROOT;AI;SKILLS', 2, 9, 0, 1, 1, 0),
(63, 'SKILLS_MARKET', 'Skills广场', 2, '/skills/market', 62, 'ROOT;AI;SKILLS;SKILLS_MARKET', 3, 1, 0, 1, 1, 0),
(64, 'BASE_TOOLS', '基础工具', 1, NULL, 6, 'ROOT;AI;BASE_TOOLS', 2, 10, 0, 1, 1, 0),
(65, 'VECTOR_DB', '向量数据库', 2, '/tools/vector-database', 64, 'ROOT;AI;BASE_TOOLS;VECTOR_DB', 3, 1, 0, 1, 1, 0),
(66, 'ENV_CHECK', '环境检查', 2, '/tools/environment-check', 64, 'ROOT;AI;BASE_TOOLS;ENV_CHECK', 3, 2, 0, 1, 1, 0),
(67, 'TREASURY_BASE', '基础管理', 1, NULL, 7, 'ROOT;TREASURY;TREASURY_BASE', 2, 1, 0, 1, 1, 0),
(68, 'TREASURY_USER', '用户管理', 2, '/treasury/basic/users', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_USER', 3, 1, 0, 1, 1, 0),
(69, 'TREASURY_MENU', '菜单管理', 2, '/treasury/basic/menus', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_MENU', 3, 2, 0, 1, 1, 0),
(70, 'TREASURY_ORG', '组织管理', 2, '/treasury/basic/organizations', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_ORG', 3, 3, 0, 1, 1, 0),
(71, 'TREASURY_SETTINGS', '基础设置', 2, '/treasury/basic/settings', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_SETTINGS', 3, 4, 0, 1, 1, 0),
(72, 'TREASURY_ACCOUNT', '账户管理', 1, NULL, 7, 'ROOT;TREASURY;TREASURY_ACCOUNT', 2, 2, 0, 1, 1, 0),
(73, 'TREASURY_SETTLE', '结算管理', 1, NULL, 7, 'ROOT;TREASURY;TREASURY_SETTLE', 2, 3, 0, 1, 1, 0);

# 租户角色模板表 系统级 由运营端维护 租户入驻时按模板初始化
CREATE TABLE `role_template` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '角色模板 ID',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '角色说明',
    `deletable` TINYINT NOT NULL DEFAULT 0 COMMENT '租户是否允许删除该角色 1-允许 0-不允许',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-启用 2-停用',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人运营用户 ID 系统初始化为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人运营用户 ID 系统初始化为空',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识 1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_template_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色模板表';

# 系统内置基础角色模板 固定 ID 仅用于系统初始化 需保留不复用
INSERT INTO `role_template` (
    `id`, `role_code`, `role_name`, `description`, `deletable`, `status`, `valid`, `version`
) VALUES
(1, 'TENANT_ADMIN', '租户管理员', '管理本租户成员 角色及租户内基础配置', 0, 1, 1, 0),
(2, 'BASIC_USER', '基础成员', '使用本租户向成员开放的基础功能', 0, 1, 1, 0);

# 系统模板角色与菜单资源的初始化关系 仅用于复制到租户 role_menu
CREATE TABLE `role_menu_template` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '主键 ID',
    `role_template_id` BIGINT UNSIGNED NOT NULL COMMENT '角色模板 ID',
    `menu_template_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单资源模板 ID',
    `include_descendants` TINYINT NOT NULL DEFAULT 0 COMMENT '是否继承子资源权限 1-是 0-否',
    `deletable` TINYINT NOT NULL DEFAULT 1 COMMENT '该角色菜单关联是否允许删除 1-允许 0-不允许',
    `creator_user_id` VARCHAR(128) DEFAULT NULL COMMENT '创建人运营用户 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_user_id` VARCHAR(128) DEFAULT NULL COMMENT '最后修改人运营用户 ID',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识 1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu_template_valid` (`role_template_id`, `menu_template_id`, `valid`),
    KEY `idx_role_menu_template_menu` (`menu_template_id`),
    CONSTRAINT `fk_role_menu_template_role` FOREIGN KEY (`role_template_id`) REFERENCES `role_template` (`id`),
    CONSTRAINT `fk_role_menu_template_menu` FOREIGN KEY (`menu_template_id`) REFERENCES `menu_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色模板菜单资源关联表';

# 租户管理员默认关联根目录并继承其子菜单 该关联不可删除
INSERT INTO `role_menu_template` (
    `id`, `role_template_id`, `menu_template_id`, `include_descendants`, `deletable`, `valid`, `version`
) VALUES
(1, 1, 1, 1, 0, 1, 0);

# 模板仅在新租户入驻时作为初始化来源
