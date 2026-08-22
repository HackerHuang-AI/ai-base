# 身份、租户、组织与权限基线模型

## 模型边界

- `base_user` 是平台全局身份，以全局唯一的 `sso_user_id` 映射 SSO 用户。
- `tenant` 是个人空间和企业租户的统一数据隔离边界。一个用户最多拥有一个个人空间，可加入多个企业租户。
- `tenant_user` 只表达成员关系；用户能否进入租户与用户在租户内有什么权限分离。
- 个人空间不能直接转换为企业租户；创建企业租户后，资源需要由用户显式迁移或复制。
- 组织、岗位和角色语义分离：组织描述协作单元，岗位描述任职，角色描述系统权限。

## 表关系

```text
base_user 1 --- N tenant_user N --- 1 tenant
tenant_user 1 --- N user_role N --- 1 role N --- N permission
tenant_user 1 --- N organization_user N --- 1 organization_info
organization_user 1 --- N organization_user_role N --- 1 role
organization_info N --- N organization_structure（由 organization_structure_node 表达树位置）
base_user 1 --- N platform_user_role N --- 1 platform_role
```

## 基础字段

所有表均使用以下基础字段：

| 字段 | 说明 |
| --- | --- |
| `ctime` | 创建时间 |
| `utime` | 更新时间 |
| `valid` | 技术逻辑有效标识：`1` 有效，`0` 软删除/废弃 |
| `version` | 乐观锁版本，初始值为 `0` |

`status` 用于业务生命周期，不与 `valid` 混用。例如成员被移除时保留 `valid = 1`，并更新业务状态为已移除。

## 租户与用户

| 表 | 职责 |
| --- | --- |
| `tenant` | 个人空间或企业租户；`owner_user_id` 是资产所有者，不是角色。企业租户必须填写唯一统一社会信用代码。 |
| `tenant_info` | 企业租户的联系人、地址和简介；个人空间不创建记录。 |
| `base_user` | 平台全局用户，不保存 `tenant_id`。 |
| `user_info` | 用户平台级低频资料。 |
| `tenant_user` | 用户是否为租户成员及成员状态，不承载角色或权限。 |

## 组织与任职

| 表 | 职责 |
| --- | --- |
| `organization_info` | 租户内组织本体，不保存上下级、负责人或排序。 |
| `organization_structure` | 组织架构定义；一个租户可有多个，有效主架构只能一个。 |
| `organization_structure_node` | 组织在某架构内的位置、父节点和同级排序。每个架构必须有且仅有一个根节点。 |
| `position` | 租户级岗位定义，不绑定具体组织。 |
| `organization_user` | 成员在组织内的单岗位任职关系及有效期。 |
| `organization_user_role` | 组织范围角色授予，例如组织负责人。 |

同一用户可在多个组织任职，但在同一组织同一时刻只能有一个有效岗位。人员调动时结束旧任职并创建新记录，不覆盖历史记录。

`organization_structure_node.sort_no` 用于同一个父节点下的展示排序；同一组织在不同组织架构中可有不同排序。根节点的 `parent_node_id` 为 `NULL`，叶子节点不需要特殊值。

组织负责人是组织范围角色，例如 `ORG_LEADER`，由 `organization_user_role` 授予；不在组织表或组织人员表中保存负责人布尔字段。一个组织可有多个负责人。

## 权限模型

| 表 | 职责 |
| --- | --- |
| `permission` | 平台统一权限点字典，定义资源和操作。 |
| `role` | 租户角色定义；`scope_type` 限制角色可在租户范围、组织范围或两者授予。 |
| `role_permission` | 租户角色与权限点关联。 |
| `user_role` | 面向租户成员的租户范围角色授予，支持有效期。 |
| `organization_user_role` | 面向组织任职的组织范围角色授予，支持有效期。 |
| `platform_role` | 跨租户的平台级管理角色，与租户角色完全分离。 |
| `platform_role_permission` | 平台角色与平台统一权限点关联。 |
| `platform_user_role` | 平台用户的平台级角色授予，支持有效期。 |

角色范围：`1-租户`、`2-组织`、`3-两者均可`。系统必须拒绝将租户范围角色写入 `organization_user_role`，或将组织范围角色写入 `user_role`。

## 归属和鉴权约定

后续所有租户业务资源必须保存非空 `tenant_id`，以它作为数据隔离与计费归属；用户创建或修改的资源还应保存 `creator_user_id`、`updater_user_id`，可转交资源额外保存 `owner_user_id`。

认证后，服务端使用 `user_id + tenant_id` 确认有效 `tenant_user`，再加载有效期内的 `user_role` 和 `organization_user_role`。业务查询必须使用服务端确认的 `tenant_id`，不得信任客户端任意传入的租户 ID。平台后台能力只使用独立的 `platform_user_role` 授权。

## 跨表约束

以下条件由应用事务和乐观锁保证：

- 一个用户最多一个有效个人空间，且 `tenant.owner_user_id` 必须是该租户有效成员。
- 每个有效组织架构仅有一个根节点，每个租户仅有一个有效主组织架构。
- 组织、组织架构节点、租户成员、岗位、角色和权限授予必须属于相同租户。
- 同一成员在同一组织同一时刻仅有一个有效任职。

