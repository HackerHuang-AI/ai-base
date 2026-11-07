CREATE DATABASE IF NOT EXISTS `ai_base` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_base`;

# 模块接口注册表 各模块维护自己的接口
CREATE TABLE `api_interface` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '接口注册主键 ID',
    `module_name` VARCHAR(128) NOT NULL COMMENT '模块应用名 取 spring.application.name',
    `http_method` VARCHAR(16) NOT NULL COMMENT 'HTTP 请求方法',
    `path` VARCHAR(256) COLLATE utf8mb4_bin NOT NULL COMMENT '服务内部接口路径或路径参数模板 不含网关模块前缀',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '接口业务说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `valid` TINYINT NOT NULL DEFAULT 1 COMMENT '有效标识 1-有效 0-无效',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_interface_module_method_path` (`module_name`, `http_method`, `path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块接口注册表';

# ai-base 前端 HTTP 接口初始化
INSERT INTO `api_interface` (`id`, `module_name`, `http_method`, `path`, `description`) VALUES
(1, 'ai-base', 'POST', '/api/access/login', '用户登录，返回会话令牌及当前空间标识'),
(2, 'ai-base', 'GET', '/api/access/sessions', '查询当前用户登录会话列表'),
(3, 'ai-base', 'POST', '/api/access/logout', '退出当前登录会话'),
(4, 'ai-base', 'POST', '/api/access/sessions/logout', '下线当前用户的指定登录会话'),
(5, 'ai-base', 'POST', '/api/access/logout-all', '退出当前用户全部登录会话'),
(6, 'ai-base', 'POST', '/api/access/sms-code/send', '发送登录短信验证码'),
(7, 'ai-base', 'POST', '/api/admin/user/page', '分页查询用户资料'),
(8, 'ai-base', 'POST', '/api/admin/user/profiles', '按用户 ID 列表批量查询用户资料'),
(9, 'ai-base', 'POST', '/api/admin/user/profiles-with-tenants', '按用户 ID 列表批量查询用户资料及所属空间'),
(10, 'ai-base', 'POST', '/api/personal/users/profile', '查询当前用户个人资料及个人空间信息'),
(11, 'ai-base', 'POST', '/api/personal/users/profile/update', '修改当前用户个人资料'),
(12, 'ai-base', 'POST', '/api/personal/users/username/update', '修改当前用户账号名'),
(13, 'ai-base', 'POST', '/api/personal/users/mobile/update', '通过短信验证码修改当前用户手机号'),
(14, 'ai-base', 'POST', '/api/personal/tenants/update', '修改当前用户个人空间名称'),
(15, 'ai-base', 'POST', '/api/tenant/user/page', '分页查询当前用户可用空间'),
(16, 'ai-base', 'POST', '/api/tenant/user/switch', '切换当前会话所在空间'),
(17, 'ai-base', 'POST', '/api/user/menu/tree', '查询当前用户可见导航菜单树'),
(18, 'ai-base', 'GET', '/api/user/tenant-preference', '查询当前用户默认空间及最近使用空间'),
(19, 'ai-base', 'POST', '/api/user/tenant-preference/update', '修改当前用户默认空间');

# 用户端前端资源模板表 系统级 由运营端维护 租户入驻时按模板初始化
CREATE TABLE `menu_template` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '前端资源模板 ID',
    `menu_code` VARCHAR(128) NOT NULL COMMENT '菜单编码 同时作为接口鉴权标识',
    `resource_name` VARCHAR(128) NOT NULL COMMENT '前端资源名称',
    `desc` VARCHAR(256) DEFAULT NULL COMMENT '菜单描述',
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
    `id`, `menu_code`, `resource_name`, `desc`, `resource_type`, `route_path`, `parent_menu_template_id`, `men_path`, `menu_level`, `sort_no`, `deletable`, `status`, `valid`, `version`
) VALUES
(1, 'ROOT', '根目录', '用户端导航菜单的根节点。', 1, NULL, NULL, 'ROOT', 0, 0, 0, 1, 1, 0),
(2, 'HOME', '首页', '用户端首页入口。', 2, '/home', 1, 'ROOT;HOME', 1, 1, 0, 1, 1, 0),
(3, 'HOME_PROFILE', '个人中心', '查看和维护个人资料。', 2, '/profile', 2, 'ROOT;HOME;HOME_PROFILE', 2, 1, 0, 1, 1, 0),
(6, 'AI', 'AI智库', '汇聚大模型、Agent与基础能力，支持智能应用构建。', 1, NULL, 1, 'ROOT;AI', 1, 2, 0, 1, 1, 0),
(7, 'TREASURY', '司库管理', '司库基础、账户与结算管理入口。', 1, NULL, 1, 'ROOT;TREASURY', 1, 3, 0, 1, 1, 0),
(8, 'DB', '数据库', '数据库功能入口。', 1, NULL, 1, 'ROOT;DB', 1, 4, 0, 1, 1, 0),
(9, 'DOCS', '企业文库', '企业文档资源入口。', 1, NULL, 1, 'ROOT;DOCS', 1, 5, 0, 1, 1, 0),
(10, 'MONITOR', '监控中心', '监控功能入口。', 1, NULL, 1, 'ROOT;MONITOR', 1, 6, 0, 1, 1, 0),
(11, 'AI_LLM', '大模型', '模型资源、管理、评测与提示词工具入口。', 1, NULL, 6, 'ROOT;AI;AI_LLM', 2, 1, 0, 1, 1, 0),
(12, 'AI_AGENT', 'Agent', 'Agent生态、开发、评测与监控入口。', 1, NULL, 6, 'ROOT;AI;AI_AGENT', 2, 2, 0, 1, 1, 0),
(13, 'AI_CAPABILITY', '基础能力', 'AI基础能力入口。', 1, NULL, 6, 'ROOT;AI;AI_CAPABILITY', 2, 3, 0, 1, 1, 0),
(14, 'AI_APP', '智能应用', '智能应用入口。', 1, NULL, 6, 'ROOT;AI;AI_APP', 2, 4, 0, 1, 1, 0),
(16, 'AI_MODEL_MANAGE', '模型管理', '模型榜单、信息、对比、参数、体验、演练与实时交互入口。', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE', 3, 1, 0, 1, 1, 0),
(19, 'AI_MODEL_RANKING', '模型榜单', '查看模型榜单。', 2, '/ai/models/rankings', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_RANKING', 4, 1, 0, 1, 1, 0),
(17, 'AI_MODEL_INFO', '模型信息', '查看模型基本信息。', 2, '/ai/models/info', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_INFO', 4, 2, 0, 1, 1, 0),
(18, 'AI_MODEL_COMPARE', '模型对比', '对比不同模型。', 2, '/ai/models/compare', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_COMPARE', 4, 3, 0, 1, 1, 0),
(74, 'AI_MODEL_PARAMS', '模型参数', '查看和管理模型参数。', 2, '/ai/models/parameters', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_PARAMS', 4, 4, 0, 1, 1, 0),
(75, 'AI_MODEL_EXPERIENCE', '模型体验', '体验和试用模型能力。', 2, '/ai/models/experience', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_EXPERIENCE', 4, 5, 0, 1, 1, 0),
(76, 'AI_MODEL_REHEARSAL', '模型演练', '开展模型演练。', 2, '/ai/models/rehearsal', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_REHEARSAL', 4, 6, 0, 1, 1, 0),
(77, 'AI_MODEL_REALTIME_INTERACTION', '实时交互', '与模型进行实时交互。', 2, '/ai/models/realtime-interaction', 16, 'ROOT;AI;AI_LLM;AI_MODEL_MANAGE;AI_MODEL_REALTIME_INTERACTION', 4, 7, 0, 1, 1, 0),
(20, 'AI_SELF_DEVELOPED_MODEL', '自研模型', '自研模型微调与部署入口。', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_SELF_DEVELOPED_MODEL', 3, 2, 0, 1, 1, 0),
(21, 'AI_MODEL_TUNE', '模型微调', '模型微调功能入口。', 2, '/ai/self-developed-models/tuning', 20, 'ROOT;AI;AI_LLM;AI_SELF_DEVELOPED_MODEL;AI_MODEL_TUNE', 4, 1, 0, 1, 1, 0),
(23, 'AI_MODEL_DEPLOY', '模型部署', '管理模型部署。', 2, '/ai/self-developed-models/deployments', 20, 'ROOT;AI;AI_LLM;AI_SELF_DEVELOPED_MODEL;AI_MODEL_DEPLOY', 4, 2, 0, 1, 1, 0),
(26, 'AI_MODEL_EVAL', '模型评测', '评测工具与对比评测入口。', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_MODEL_EVAL', 3, 3, 0, 1, 1, 0),
(29, 'AI_EVAL_TOOL', '评测工具', '使用评测工具开展模型评测。', 2, '/ai/evaluation/tools', 26, 'ROOT;AI;AI_LLM;AI_MODEL_EVAL;AI_EVAL_TOOL', 4, 1, 0, 1, 1, 0),
(28, 'AI_COMPARE_EVAL', '对比评测', '开展模型对比评测。', 2, '/ai/evaluation/compare', 26, 'ROOT;AI;AI_LLM;AI_MODEL_EVAL;AI_COMPARE_EVAL', 4, 2, 0, 1, 1, 0),
(30, 'AI_PROMPT', '提示词工厂', '提示词浏览与优化入口。', 1, NULL, 11, 'ROOT;AI;AI_LLM;AI_PROMPT', 3, 4, 0, 1, 1, 0),
(31, 'AI_PROMPT_MARKET', '提示词市场', '浏览提示词资源。', 2, '/ai/prompts/market', 30, 'ROOT;AI;AI_LLM;AI_PROMPT;AI_PROMPT_MARKET', 4, 1, 0, 1, 1, 0),
(32, 'AI_PROMPT_OPTIMIZE', '提示词优化', '优化提示词。', 2, '/ai/prompts/optimize', 30, 'ROOT;AI;AI_LLM;AI_PROMPT;AI_PROMPT_OPTIMIZE', 4, 2, 0, 1, 1, 0),
(33, 'AGENT_ECO', 'Agent生态', 'Agent浏览、注册与管理大盘入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_ECO', 3, 1, 0, 1, 1, 0),
(34, 'AGENT_MARKET', 'Agent广场', '浏览Agent资源。', 2, '/agent/ecosystem/market', 33, 'ROOT;AI;AI_AGENT;AGENT_ECO;AGENT_MARKET', 4, 1, 0, 1, 1, 0),
(35, 'AGENT_REGISTER', 'Agent注册', '注册Agent。', 2, '/agent/ecosystem/register', 33, 'ROOT;AI;AI_AGENT;AGENT_ECO;AGENT_REGISTER', 4, 2, 0, 1, 1, 0),
(36, 'AGENT_DASHBOARD', 'Agent管理大盘', '查看Agent管理概览。', 2, '/agent/ecosystem/dashboard', 33, 'ROOT;AI;AI_AGENT;AGENT_ECO;AGENT_DASHBOARD', 4, 3, 0, 1, 1, 0),
(37, 'AGENT_DEV', 'Agent开发', 'Agent配置、工作流与工具管理入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_DEV', 3, 2, 0, 1, 1, 0),
(38, 'AGENT_CONFIG', 'Agent配置', '管理Agent配置。', 2, '/agent/development/config', 37, 'ROOT;AI;AI_AGENT;AGENT_DEV;AGENT_CONFIG', 4, 1, 0, 1, 1, 0),
(39, 'AGENT_WORKFLOW', '工作流', '管理Agent工作流。', 2, '/agent/development/workflows', 37, 'ROOT;AI;AI_AGENT;AGENT_DEV;AGENT_WORKFLOW', 4, 2, 0, 1, 1, 0),
(40, 'AGENT_TOOL', '工具', '管理Agent开发工具。', 2, '/agent/development/tools', 37, 'ROOT;AI;AI_AGENT;AGENT_DEV;AGENT_TOOL', 4, 3, 0, 1, 1, 0),
(41, 'AGENT_EVAL', 'Agent评测', 'Agent评测任务、指标、评测集与测试对象入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_EVAL', 3, 3, 0, 1, 1, 0),
(42, 'AGENT_EVAL_TASK', '评测任务', '管理Agent评测任务。', 2, '/agent/evaluation/tasks', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_EVAL_TASK', 4, 1, 0, 1, 1, 0),
(43, 'AGENT_EVAL_METRIC', '评测指标', '管理Agent评测指标。', 2, '/agent/evaluation/metrics', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_EVAL_METRIC', 4, 2, 0, 1, 1, 0),
(44, 'AGENT_EVAL_SET', '评测集', '管理Agent评测集。', 2, '/agent/evaluation/sets', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_EVAL_SET', 4, 3, 0, 1, 1, 0),
(45, 'AGENT_TEST_OBJECT', '测试对象', '管理Agent评测的测试对象。', 2, '/agent/evaluation/objects', 41, 'ROOT;AI;AI_AGENT;AGENT_EVAL;AGENT_TEST_OBJECT', 4, 4, 0, 1, 1, 0),
(46, 'AGENT_OBSERVE', 'Agent监控', 'Agent链路、指标与自动化任务入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE', 3, 4, 0, 1, 1, 0),
(47, 'AGENT_TRACE', 'Trace查询', '查询Agent调用链路。', 2, '/agent/observability/traces', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_TRACE', 4, 1, 0, 1, 1, 0),
(48, 'AGENT_TRACE_MANAGE', 'Trace管理', '管理Agent调用链路。', 2, '/agent/observability/trace-management', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_TRACE_MANAGE', 4, 2, 0, 1, 1, 0),
(49, 'AGENT_STAT', '指标监控', '查看Agent监控指标。', 2, '/agent/observability/metrics', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_STAT', 4, 3, 0, 1, 1, 0),
(50, 'AGENT_AUTO_TASK', '自动化任务', '管理Agent监控自动化任务。', 2, '/agent/observability/automation', 46, 'ROOT;AI;AI_AGENT;AGENT_OBSERVE;AGENT_AUTO_TASK', 4, 4, 0, 1, 1, 0),
(51, 'KNOWLEDGE_BASE', '知识库', '知识管理、检索测试与知识库资源入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;KNOWLEDGE_BASE', 3, 5, 0, 1, 1, 0),
(52, 'KNOWLEDGE_MANAGE', '知识管理', '管理知识内容。', 2, '/knowledge/management', 51, 'ROOT;AI;AI_AGENT;KNOWLEDGE_BASE;KNOWLEDGE_MANAGE', 4, 1, 0, 1, 1, 0),
(53, 'KNOWLEDGE_SEARCH', '检索测试', '测试知识检索效果。', 2, '/knowledge/search-test', 51, 'ROOT;AI;AI_AGENT;KNOWLEDGE_BASE;KNOWLEDGE_SEARCH', 4, 2, 0, 1, 1, 0),
(54, 'KNOWLEDGE_LIB', '知识库列表', '查看知识库列表。', 2, '/knowledge/libraries', 51, 'ROOT;AI;AI_AGENT;KNOWLEDGE_BASE;KNOWLEDGE_LIB', 4, 3, 0, 1, 1, 0),
(55, 'DATA_AGENT_KB', 'DataAgent知识库', '管理DataAgent知识库。', 2, '/knowledge/data-agent', 51, 'ROOT;AI;AI_AGENT;KNOWLEDGE_BASE;DATA_AGENT_KB', 4, 4, 0, 1, 1, 0),
(56, 'MCP', 'MCP', 'MCP调试、工具资源与体验入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;MCP', 3, 6, 0, 1, 1, 0),
(57, 'MCP_DEBUG', 'MCP调试', '调试MCP服务与工具。', 2, '/mcp/debug', 56, 'ROOT;AI;AI_AGENT;MCP;MCP_DEBUG', 4, 1, 0, 1, 1, 0),
(58, 'TOOL_POOL', 'Tool池', '查看和管理MCP工具资源。', 2, '/mcp/tool-pool', 56, 'ROOT;AI;AI_AGENT;MCP;TOOL_POOL', 4, 2, 0, 1, 1, 0),
(59, 'MCP_PLAYGROUND', 'MCP Playground', '体验和测试MCP工具调用。', 2, '/mcp/playground', 56, 'ROOT;AI;AI_AGENT;MCP;MCP_PLAYGROUND', 4, 3, 0, 1, 1, 0),
(60, 'MEMORY', '记忆', '记忆功能入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;MEMORY', 3, 7, 0, 1, 1, 0),
(61, 'MEMORY_MANAGE', '记忆管理', '管理记忆内容。', 2, '/memory/management', 60, 'ROOT;AI;AI_AGENT;MEMORY;MEMORY_MANAGE', 4, 1, 0, 1, 1, 0),
(62, 'SKILLS', 'Skills', 'Skills资源入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;SKILLS', 3, 8, 0, 1, 1, 0),
(63, 'SKILLS_MARKET', 'Skills广场', '浏览Skills资源。', 2, '/skills/market', 62, 'ROOT;AI;AI_AGENT;SKILLS;SKILLS_MARKET', 4, 1, 0, 1, 1, 0),
(64, 'TOOLS', '工具', '向量数据库与环境检查工具入口。', 1, NULL, 12, 'ROOT;AI;AI_AGENT;TOOLS', 3, 9, 0, 1, 1, 0),
(65, 'VECTOR_DB', '向量数据库', '向量数据库工具入口。', 2, '/tools/vector-database', 64, 'ROOT;AI;AI_AGENT;TOOLS;VECTOR_DB', 4, 1, 0, 1, 1, 0),
(66, 'ENV_CHECK', '环境检查', '检查运行环境。', 2, '/tools/environment-check', 64, 'ROOT;AI;AI_AGENT;TOOLS;ENV_CHECK', 4, 2, 0, 1, 1, 0),
(67, 'TREASURY_BASE', '基础管理', '司库用户、菜单、组织与基础设置入口。', 1, NULL, 7, 'ROOT;TREASURY;TREASURY_BASE', 2, 1, 0, 1, 1, 0),
(68, 'TREASURY_USER', '用户管理', '管理司库用户。', 2, '/treasury/basic/users', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_USER', 3, 1, 0, 1, 1, 0),
(69, 'TREASURY_MENU', '菜单管理', '管理司库菜单。', 2, '/treasury/basic/menus', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_MENU', 3, 2, 0, 1, 1, 0),
(70, 'TREASURY_ORG', '组织管理', '管理司库组织。', 2, '/treasury/basic/organizations', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_ORG', 3, 3, 0, 1, 1, 0),
(71, 'TREASURY_SETTINGS', '基础设置', '维护司库基础设置。', 2, '/treasury/basic/settings', 67, 'ROOT;TREASURY;TREASURY_BASE;TREASURY_SETTINGS', 3, 4, 0, 1, 1, 0),
(72, 'TREASURY_ACCOUNT', '账户管理', '司库账户管理入口。', 1, NULL, 7, 'ROOT;TREASURY;TREASURY_ACCOUNT', 2, 2, 0, 1, 1, 0),
(73, 'TREASURY_SETTLE', '结算管理', '司库结算管理入口。', 1, NULL, 7, 'ROOT;TREASURY;TREASURY_SETTLE', 2, 3, 0, 1, 1, 0);

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
