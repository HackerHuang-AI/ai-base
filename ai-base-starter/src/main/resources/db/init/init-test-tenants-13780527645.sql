-- 人工执行：为账户 13780527645 初始化两个企业测试租户及角色、菜单授权。
-- 前置：已完成系统模板、用户表建表及该账户初始化；仅用于测试环境，执行一次。
USE `ai_base`;

START TRANSACTION;

-- 租户基础信息。
INSERT INTO `tenant` (`id`, `tenant_id`, `tenant_name`, `tenant_type`, `credit_code`, `status`, `valid`, `version`) VALUES
(2104843632644194309, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', '多租户测试空间 A', 2, NULL, 1, 1, 0),
(2104843632644194310, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', '多租户测试空间 B', 2, NULL, 1, 1, 0);

-- 租户详情。
INSERT INTO `tenant_info` (`id`, `tenant_id`, `contact_name`, `description`, `valid`, `version`) VALUES
(2104843632644194311, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', '测试联系人 A', '用于企业空间 A 的切换与权限测试', 1, 0),
(2104843632644194312, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', '测试联系人 B', '用于企业空间 B 的切换与权限测试', 1, 0);

-- 两个租户的首位管理员成员关系。
INSERT INTO `tenant_user` (`id`, `tenant_id`, `user_id`, `status`, `joined_at`, `creator_user_id`, `updater_user_id`, `valid`, `version`) VALUES
(2104843632644194313, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 1, NOW(), 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 1, 0),
(2104843632644194314, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 1, NOW(), 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 1, 0);

-- 租户菜单从现有模板复制，父菜单 ID 同步映射，按层级先插入父节点。
INSERT INTO `tenant_menu` (`id`, `tenant_id`, `menu_code`, `resource_name`, `resource_type`, `route_path`, `parent_tenant_menu_id`, `men_path`, `menu_level`, `icon`, `sort_no`, `deletable`, `status`, `valid`, `version`)
SELECT 2104843632644200000 + id, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182',
       menu_code, resource_name, resource_type, route_path, 2104843632644200000 + parent_menu_template_id,
       men_path, menu_level, icon, sort_no, deletable, status, valid, 0
FROM `menu_template`
ORDER BY menu_level, id;

INSERT INTO `tenant_menu` (`id`, `tenant_id`, `menu_code`, `resource_name`, `resource_type`, `route_path`, `parent_tenant_menu_id`, `men_path`, `menu_level`, `icon`, `sort_no`, `deletable`, `status`, `valid`, `version`)
SELECT 2104843632644201000 + id, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e',
       menu_code, resource_name, resource_type, route_path, 2104843632644201000 + parent_menu_template_id,
       men_path, menu_level, icon, sort_no, deletable, status, valid, 0
FROM `menu_template`
ORDER BY menu_level, id;

-- 每个租户初始化租户管理员、基础成员两个内置角色。
INSERT INTO `role` (`id`, `tenant_id`, `role_code`, `role_name`, `description`, `deletable`, `status`, `valid`, `version`) VALUES
(2104843632644194315, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', 'TENANT_ADMIN', '租户管理员', '管理本租户成员 角色及租户内基础配置', 0, 1, 1, 0),
(2104843632644194316, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', 'BASIC_USER', '基础成员', '使用本租户向成员开放的基础功能', 0, 1, 1, 0),
(2104843632644194317, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', 'TENANT_ADMIN', '租户管理员', '管理本租户成员 角色及租户内基础配置', 0, 1, 1, 0),
(2104843632644194318, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', 'BASIC_USER', '基础成员', '使用本租户向成员开放的基础功能', 0, 1, 1, 0);

-- 按现有角色菜单模板：管理员关联 ROOT（模板 ID 为 1），继承全部子菜单，不可删除。
INSERT INTO `role_menu` (`id`, `tenant_id`, `role_id`, `tenant_menu_id`, `include_descendants`, `deletable`, `valid`, `version`) VALUES
(2104843632644194319, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', 2104843632644194315, 2104843632644200001, 1, 0, 1, 0),
(2104843632644194320, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', 2104843632644194317, 2104843632644201001, 1, 0, 1, 0);

-- 为当前用户授予两个租户的管理员角色，授权无失效时间。
INSERT INTO `user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `effective_start_time`, `effective_end_time`, `creator_user_id`, `valid`, `version`) VALUES
(2104843632644194321, 'ten_5e9d540d-4bfa-4564-a68c-49fd1570e182', 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 2104843632644194315, NOW(), NULL, 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 1, 0),
(2104843632644194322, 'ten_94d3f14e-0118-47c0-bd33-f8254160436e', 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 2104843632644194317, NOW(), NULL, 'usr_01a0ec2d-6e59-7b03-a166-737bb077d6e3', 1, 0);

COMMIT;

