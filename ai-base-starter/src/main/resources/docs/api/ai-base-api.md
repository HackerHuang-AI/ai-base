# ai-base 接口文档

## 请求与返回约定

- 以下 19 条接口按接口注册表顺序列出，路径均为完整网关路径。
- 登录、发送短信无需认证；其余接口均须携带 `Authorization: Bearer <sessionToken>` 和唯一的 `X-Tenant-Id`，空间标识须与会话当前空间一致。
- 仅有 JSON 请求体的接口需要 `Content-Type: application/json`。字段非必填不代表请求体可省略；标记 JSON 请求体的接口必须提供对象，可无必填字段时传 `{}`。
- HTTP 401：未认证或登录失效；403：空间头缺失、重复或不匹配；400：请求参数或业务错误（含登录失败）；500：系统错误。
- 入参表列请求体字段，无查询参数的接口会明确注明。出参表除注明 `data` 外，字段均位于成功响应的 `data` 内；`[]` 表示数组元素层级，不是字段名。
- 标记“可为 null”的响应字段仍保留字段名；未标记的成功响应字段非 null。日期时间为 string，采用无时区本地时间，如 `2026-11-02T10:30:00`，可带小数秒。
- 必填 string 不接受 null、空串或纯空白。未注明的手机号、邮箱、账号及 URL 不额外限制格式或长度。

### 统一返回包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | string | 成功为 `00`，失败为 `01` |
| message | string | 成功为“成功”，失败为错误说明 |
| data | object/array | 业务数据，结构见各接口；无数据或失败时为 null |
| errorCode | string | 主错误码；成功时省略，失败时返回 |
| errors | array | 错误明细对象数组，可为空数组；成功时省略 |
| errors.errorCode | string | 每个明细对象的错误码 |
| errors.message | string | 每个明细对象的错误说明 |

## 1. POST /ai-base/api/access/login

业务说明：用户登录，返回会话令牌及当前空间标识；支持账号密码、短信登录，同设备登录替换旧会话，扫码未开放。

### 入参

JSON 请求体。可选请求头 `User-Agent`（string）：客户端描述，未提供时会话对应字段为 null。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| loginType | string | 是 | `ACCOUNT_PASSWORD` 或 `MOBILE_SMS`；不区分大小写，不去除前后空格；`APP_QR_CONFIRM` 未开放 |
| account | string | 账号密码登录必填 | 登录账号；其他方式可省略或 null |
| password | string | 账号密码登录必填 | 登录密码；其他方式可省略或 null |
| mobile | string | 短信登录必填 | 手机号；其他方式可省略或 null |
| smsCode | string | 短信登录必填 | 有效一次性验证码，保留前导零；其他方式可省略或 null |
| qrLoginId | string | 未开放扫码方式条件必填 | 仅 `APP_QR_CONFIRM` 要求非空白；提供后仍失败，其他方式可省略或 null |
| deviceId | string | 是 | 安装实例标识，16～128 位，仅英文字母、数字、下划线、短横线 |

### 出参

`data` 为 object。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sessionToken | string | 会话访问令牌，后续用于 Bearer 认证 |
| tenantId | string | 登录选中的当前空间标识，后续用于空间请求头 |

## 2. GET /ai-base/api/access/sessions

业务说明：查询当前用户有效登录会话列表，按登录时间倒序返回，不返回令牌。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 无 | — | — | 无业务入参，使用统一认证请求头 |

### 出参

`data` 为 array，每个元素是会话 object；无有效会话时为 `[]`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| [].sessionId | string | 会话标识，可为 null |
| [].deviceId | string | 登录时的安装实例标识，可为 null |
| [].loginIp | string | 登录 IP 原值，可为 null |
| [].userAgent | string | 客户端描述，可为 null |
| [].loginAt | string | 登录时间，可为 null |
| [].current | boolean | 是否为当前请求使用的会话 |

## 3. POST /ai-base/api/access/logout

业务说明：退出当前登录会话，成功后原会话令牌失效。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 无 | — | — | 无业务入参，使用统一认证请求头 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 4. POST /ai-base/api/access/sessions/logout

业务说明：下线当前用户的指定登录会话；会话不存在、不可读或不属于本人时不处理，仍返回成功。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | string | 是 | 待下线会话标识，取自会话列表 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 5. POST /ai-base/api/access/logout-all

业务说明：退出当前用户全部登录会话，包含当前会话；无会话时仍成功。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 无 | — | — | 无业务入参，使用统一认证请求头 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 6. POST /ai-base/api/access/sms-code/send

业务说明：发送登录短信验证码，有效期 5 分钟，同号间隔 60 秒，不回传验证码；dev/test 仅模拟发送，stag/prod 暂不可用。

### 入参

JSON 请求体，无需认证。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| mobile | string | 是 | 接收验证码的手机号 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，不返回验证码 |

## 7. POST /ai-base/api/admin/user/page

业务说明：分页查询有效用户资料，支持资料、状态及创建时间筛选，按创建时间倒序返回。

### 入参

JSON 请求体，可传 `{}`。字符串筛选省略、null 或空串表示不筛选；时间及状态省略或 null 表示不筛选。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| pageNo | integer | 否 | 省略默认 1，最小 1；显式 null 不使用默认，返回 500 |
| pageSize | integer | 否 | 省略默认 10，范围 1～100；显式 null 不使用默认，返回 500 |
| userId | string | 否 | 全局用户标识，精确匹配 |
| mobile | string | 否 | 手机号，包含匹配 |
| email | string | 否 | 邮箱，包含匹配 |
| username | string | 否 | 账号名，包含匹配 |
| name | string | 否 | 姓名，包含匹配 |
| avatarUrl | string | 否 | 头像地址，包含匹配 |
| jobNumber | string | 否 | 工号，包含匹配 |
| status | integer | 否 | 状态精确匹配；1 正常、2 禁用，不限制其他整数 |
| createdStartTime | string | 否 | 创建时间下界，包含边界；无起止顺序校验 |
| createdEndTime | string | 否 | 创建时间上界，包含边界；无起止顺序校验 |

### 出参

`data` 为分页 object；`records` 为用户资料数组，以下展平列出元素全部字段。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| records | array | 本页用户资料对象数组，无记录为 `[]` |
| records[].userId | string | 全局用户标识，可为 null |
| records[].mobile | string | 手机号，可为 null |
| records[].email | string | 邮箱，可为 null |
| records[].username | string | 账号名，可为 null |
| records[].name | string | 姓名，可为 null |
| records[].avatarUrl | string | 头像地址，可为 null |
| records[].jobNumber | string | 工号，可为 null |
| total | integer | 符合条件的总记录数 |
| pageNo | integer | 当前页码 |
| pageSize | integer | 每页条数 |

## 8. POST /ai-base/api/admin/user/profiles

业务说明：按用户 ID 列表批量查询有效用户资料，按请求顺序去重；不存在或无效用户省略，不补占位。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| userIds | array | 是 | 非空 string 数组，元素必填非空白；允许重复，无条数上限校验 |

### 出参

`data` 为 array，每个元素是用户资料 object；无匹配用户时为 `[]`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| [].userId | string | 全局用户标识，可为 null |
| [].mobile | string | 手机号，可为 null |
| [].email | string | 邮箱，可为 null |
| [].username | string | 账号名，可为 null |
| [].name | string | 姓名，可为 null |
| [].avatarUrl | string | 头像地址，可为 null |
| [].jobNumber | string | 工号，可为 null |

## 9. POST /ai-base/api/admin/user/profiles-with-tenants

业务说明：按用户 ID 列表批量查询有效用户资料及所属可用空间，按请求顺序去重；未找到用户不补占位。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| userIds | array | 是 | 非空 string 数组，元素必填非空白；允许重复，无条数上限校验 |

### 出参

`data` 为 array，每个元素含平铺用户资料及嵌套 `tenants` 数组；无匹配用户时为 `[]`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| [].userId | string | 全局用户标识，可为 null |
| [].mobile | string | 手机号，可为 null |
| [].email | string | 邮箱，可为 null |
| [].username | string | 账号名，可为 null |
| [].name | string | 姓名，可为 null |
| [].avatarUrl | string | 头像地址，可为 null |
| [].jobNumber | string | 工号，可为 null |
| [].tenants | array | 所属可用空间对象数组，无空间为 `[]` |
| [].tenants[].tenantId | string | 空间标识，可为 null |
| [].tenants[].tenantName | string | 空间名称，可为 null |
| [].tenants[].tenantType | integer | 1 个人空间、2 企业租户；可为其他值或 null |
| [].tenants[].tenantTypeDescription | string | 个人空间/企业租户；未知类型或 null 类型时为 null |

## 10. POST /ai-base/api/personal/users/profile

业务说明：查询当前用户个人资料及个人空间信息；个人空间不一定是会话当前空间，用户或个人空间不存在时失败。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 无 | — | — | 无业务入参，使用统一认证请求头 |

### 出参

`data` 为 object，用户资料平铺，`personalTenant` 为嵌套 object，成功时非 null。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | string | 全局用户标识，可为 null |
| mobile | string | 手机号，可为 null |
| email | string | 邮箱，可为 null |
| username | string | 账号名，可为 null |
| name | string | 姓名，可为 null |
| avatarUrl | string | 头像地址，可为 null |
| jobNumber | string | 工号，可为 null |
| personalTenant | object | 本人的个人空间 |
| personalTenant.tenantId | string | 个人空间标识，可为 null |
| personalTenant.tenantName | string | 个人空间名称，可为 null |
| personalTenant.tenantType | integer | 个人空间类型 1 |
| personalTenant.tenantTypeDescription | string | 个人空间 |

## 11. POST /ai-base/api/personal/users/profile/update

业务说明：修改当前用户邮箱、姓名、头像及工号，不修改账号、手机号或密码；省略或 null 不承诺清空原值。

### 入参

JSON 请求体，可传 `{}`；所有字段均可省略或 null。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| email | string | 否 | 邮箱，不校验格式 |
| name | string | 否 | 姓名 |
| avatarUrl | string | 否 | 头像地址，不校验 URL 格式 |
| jobNumber | string | 否 | 工号 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 12. POST /ai-base/api/personal/users/username/update

业务说明：修改当前用户账号名，账号须未被占用；实际变更成功后退出全部会话，需重新登录；与原值相同则直接成功。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 新账号名，无额外格式或长度校验 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 13. POST /ai-base/api/personal/users/mobile/update

业务说明：通过短信验证码修改手机号，号码须未被占用；变更成功后需重新登录；号码不变则直接成功，不消费验证码。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| mobile | string | 是 | 新手机号，无额外格式或长度校验 |
| smsCode | string | 是 | 新手机号收到的有效一次性验证码；即使号码不变仍须非空白 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 14. POST /ai-base/api/personal/tenants/update

业务说明：修改当前用户个人空间名称，与会话当前空间无关；个人空间不存在或更新失败时返回业务错误。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| tenantName | string | 是 | 个人空间名称，须非空白，无最大长度校验 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

## 15. POST /ai-base/api/tenant/user/page

业务说明：分页查询当前用户可用空间，按空间类型及空间关系顺序升序返回。

### 入参

JSON 请求体，可传 `{}`。分页字段省略使用默认值，显式 null 不使用默认值，返回 400。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| pageNo | integer | 否 | 省略默认 1，最小 1；显式 null 返回页码错误 |
| pageSize | integer | 否 | 省略默认 10，范围 1～100；显式 null 返回每页条数错误 |

### 出参

`data` 为分页 object；`records` 为空间数组，以下展平列出元素全部字段。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| records | array | 本页可用空间对象数组，无记录为 `[]` |
| records[].tenantId | string | 空间标识，可为 null |
| records[].tenantName | string | 空间名称，可为 null |
| records[].tenantType | integer | 1 个人空间、2 企业租户；可为其他值或 null |
| records[].tenantTypeDescription | string | 个人空间/企业租户；未知类型或 null 类型时为 null |
| total | integer | 当前用户可用空间总数 |
| pageNo | integer | 当前页码 |
| pageSize | integer | 每页条数 |

## 16. POST /ai-base/api/tenant/user/switch

业务说明：切换当前会话空间，请求头传旧空间、请求体传目标空间；成功后后续请求头改用返回的新空间，令牌不变。

### 入参

JSON 请求体。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| tenantId | string | 是 | 目标可用空间，最长 128 位，仅小写字母、数字、下划线、短横线 |

### 出参

`data` 为 object，表示切换后的空间，不返回新令牌。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| tenantId | string | 切换后的当前空间标识 |
| tenantName | string | 空间名称，可为 null |
| tenantType | integer | 1 个人空间、2 企业租户；可为其他值或 null |
| tenantTypeDescription | string | 个人空间/企业租户；未知类型或 null 类型时为 null |

## 17. POST /ai-base/api/user/menu/tree

业务说明：查询当前用户在当前空间可见的导航菜单树，过滤未授权页面与空目录，展开 ROOT 根目录。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 无 | — | — | 无业务入参，使用统一认证请求头 |

### 出参

`data` 为 array，每个元素是菜单节点 object；无可见菜单时为 `[]`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| [].menuCode | string | 当前空间内菜单编码，可为 null |
| [].resourceName | string | 导航展示名称，可为 null |
| [].resourceType | integer | 1 目录、2 页面 |
| [].routePath | string | 页面路由，目录可为空串或 null |
| [].icon | string | 图标标识，可为空串或 null |
| [].sortNo | integer | 同级升序排序值，可为 null |
| [].children | array | 子节点对象数组，每个元素递归包含本表全部 7 个字段；叶子为 `[]`，非 null |

## 18. GET /ai-base/api/user/tenant-preference

业务说明：查询当前用户默认空间及最近使用空间，不重新校验空间可用性；无偏好记录时 data 为 null。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 无 | — | — | 无业务入参，使用统一认证请求头 |

### 出参

`data` 为 object 或 null；有记录时对象包含以下字段。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| userId | string | 当前用户标识，可为 null |
| defaultTenantId | string | 默认空间标识，未设置或已清除时为 null |
| lastUsedTenantId | string | 最近登录或切换使用的空间标识，可为 null |

## 19. POST /ai-base/api/user/tenant-preference/update

业务说明：设置或清除当前用户默认空间，不切换当前会话；默认空间下次登录优先使用。

### 入参

JSON 请求体，可传 `{}`；省略 `defaultTenantId` 与显式 null 都表示清除，不是保持原值。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| defaultTenantId | string | 否 | 可省略或 null；非 null 须为本人可用空间，最长 128 位，仅小写字母、数字、下划线、短横线；空串或纯空白报错 |

### 出参

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data | — | 为 null，无业务返回字段 |

