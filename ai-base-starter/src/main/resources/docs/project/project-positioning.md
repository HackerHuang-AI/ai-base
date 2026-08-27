# ai-base 项目定位

## 一、平台整体模块划分

AI 智能能力管理平台按基础域、运行时引擎和治理分析域拆分：

| 模块 | 平台职责 | 当前建设状态 |
| --- | --- | --- |
| `ai-base` | 用户、租户、组织、权限与菜单基础域 | 数据模型、迁移脚本、实体与 Mapper 已具备；业务服务和 SSO 待建设 |
| `ai-gateway` | 统一流量入口、认证、路由与追踪 | TraceId、访问日志、JWT 过滤器已实现；路由和 SSO 治理待完善 |
| `ai-agent` | 多 LLM 平台协议适配 | 多平台文本、流式、多模态和 Function Calling 已具备 |
| `ai-mcp` | 内置工具与外部 MCP Server 接入 | 工具路由、MCP Client、SSE/STDIO 接入已具备 |
| `ai-orchestration` | Agent 任务编排与 ReAct 循环 | DTO、Facade 和技术底座已具备；核心编排未实现 |
| `ai-memory` | 会话上下文与长期记忆 | DTO、Facade 和技术底座已具备；存储与检索未实现 |
| `ai-knowledge` | 知识库与 RAG | 知识库、文档、解析、向量化、Milvus/MinIO 接入已具备 |
| `ai-eval` | 模型与 Agent 效果评估 | DTO、Facade 和技术底座已具备；评测执行未实现 |
| `ai-experiment` | 实验分流与版本对比 | DTO、Facade 和技术底座已具备；分流与分析未实现 |
| `ai-analysis` | 当前代码评审/静态分析，后续平台运行分析 | 多代码平台适配、PR 分析及多阶段部署编排已具备 |

## 二、项目背景与定位

`ai-base` 是平台基础域服务。平台的所有租户级资源都应以 `tenant_id` 隔离，用户私有资源还应关联 `user_id`；因此它是后续会话、知识库、Agent 和分析数据可安全归属的前提。

本模块不负责模型调用、工具执行、RAG 或会话记忆，只负责提供可信的身份、组织和权限上下文。

## 三、本模块功能定位

- 管理全局用户及后续 SSO 身份映射；
- 管理个人空间、企业租户、成员关系与企业入驻审核；
- 管理组织架构、岗位、租户角色、平台角色、权限和菜单；
- 为其他模块提供 `tenant_id`、`user_id`、成员资格和权限校验基础。

## 四、当前建设完整度

- 已完成：四层 Maven 骨架、统一异常/响应/国际化/TraceId、基础配置；租户、用户、组织、角色和权限的迁移脚本、实体及 Mapper。
- 待完成：应用服务、Controller/Dubbo Facade、企业 SSO、登录态与网关身份联动、实际授权校验。

## 五、主要技术栈

| 功能 | 关键技术栈 |
| --- | --- |
| 基础身份与权限数据 | MySQL、Flyway、MyBatis-Plus |
| SSO 与跨服务身份上下文 | 企业 SSO、Dubbo Triple、Nacos |
| Web 基础治理 | Spring MVC Interceptor、MDC、TraceId |

