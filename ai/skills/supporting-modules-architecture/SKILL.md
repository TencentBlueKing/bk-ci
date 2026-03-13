---
name: supporting-modules-architecture
description: BK-CI 支撑模块开发指南 — 配置凭证(Ticket)、管理构建机(Environment)、设置通知规则(Notify)、查询构建日志(Log)、配置质量红线(Quality)、对接开放接口(OpenAPI)。当需要为流水线添加凭证访问、环境管理、通知策略、日志查询、质量门禁或第三方 API 集成时使用本 Skill。
related_skills:
  - 00-bkci-global-architecture
  - process-module-architecture
token_estimate: 15000
---

# 支撑模块架构指南

## Skill 概述

本 Skill 涵盖 BK-CI 的 **6 大支撑模块**，为核心流水线引擎提供凭证、环境、通知、日志、质量检查、API 开放等关键能力。

| 模块 | 典型任务 | 参考文档 |
|------|----------|----------|
| **Ticket** | 创建 SSH/Token 凭证、配置凭证授权 | [1-ticket-module.md](./reference/1-ticket-module.md) |
| **Environment** | 注册构建机节点、配置环境分组 | [2-environment-module.md](./reference/2-environment-module.md) |
| **Notify** | 设置流水线失败邮件通知、配置企微审批提醒 | [3-notify-module.md](./reference/3-notify-module.md) |
| **Log** | 实时查看构建日志、按关键字搜索日志 | [4-log-module.md](./reference/4-log-module.md) |
| **Quality** | 添加代码覆盖率红线、配置安全扫描拦截 | [5-quality-module.md](./reference/5-quality-module.md) |
| **OpenAPI** | 第三方系统触发流水线、通过 SDK 查询构建状态 | [6-openapi-module.md](./reference/6-openapi-module.md) |

---

## 核心业务模块 vs 支撑模块

| 维度 | 核心业务模块 | 支撑服务模块 |
|------|-------------|-------------|
| **职责** | 流水线编排、执行、调度、存储 | 提供凭证、环境、通知、日志、质量、API 能力 |
| **耦合度** | 强耦合，相互依赖 | 松耦合，被动提供服务 |
| **调用方向** | 相互调用，协同执行 | 被核心模块调用 |
| **复杂度** | 高 (流程复杂) | 中低 (功能单一) |
| **独立性** | 低 (需协作) | 高 (独立服务) |

---

## 支撑模块协作全景

```
┌─────────────────────────────────────────────────────────────┐
│                    核心业务层                                │
│   Process / Auth / Project / Repository / Store /          │
│   Artifactory / Dispatch / Worker / Agent                  │
└─────────────────────────────────────────────────────────────┘
           │         │         │         │         │
           ↓         ↓         ↓         ↓         ↓
    ┌──────────┬──────────┬──────────┬──────────┬──────────┐
    │  Ticket  │  Env     │  Notify  │  Log     │  Quality │
    │  凭证管理 │  环境管理 │  通知服务 │  日志服务 │  质量检查 │
    └──────────┴──────────┴──────────┴──────────┴──────────┘
                                │
                                ↓
                        ┌──────────────┐
                        │   OpenAPI    │
                        │   API 网关    │
                        └──────────────┘
                                │
                                ↓
                          第三方系统
```

---

## 一、凭证管理模块 (Ticket)

详见 [reference/1-ticket-module.md](./reference/1-ticket-module.md)

### 模块定位

负责管理各类凭证（Credential）和证书（Cert），为代码库、构建机、部署等场景提供安全的凭证存储和访问服务。

### 核心功能

- 凭证类型管理（密码/SSH/Token/证书）
- 加密存储与安全访问
- 凭证授权与权限控制
- 凭证使用审计

### 典型场景

- 流水线拉取代码（使用 Git 凭证）
- 构建机 SSH 连接（使用 SSH 凭证）
- 部署到远程服务器（使用密码/证书）

---

## 二、构建机环境模块 (Environment)

详见 [reference/2-environment-module.md](./reference/2-environment-module.md)

### 模块定位

负责管理构建节点（Node）、构建环境（Env）、第三方构建机（Agent）的注册、状态监控、分组等功能。

### 核心功能

- 构建机节点管理
- 环境变量配置
- 节点状态监控
- 第三方构建机接入

### 典型场景

- 管理公共构建机池
- 配置项目专属构建环境
- 接入企业自建构建机

---

## 三、通知服务模块 (Notify)

详见 [reference/3-notify-module.md](./reference/3-notify-module.md)

### 模块定位

负责向用户发送各类通知消息，支持多种通知渠道（邮件/企微/RTX/短信）。

### 核心功能

- 多渠道通知发送
- 通知模板管理
- 订阅管理
- 通知策略配置

### 典型场景

- 流水线成功/失败通知
- 审批流程通知
- 质量红线拦截通知

---

## 四、构建日志模块 (Log)

详见 [reference/4-log-module.md](./reference/4-log-module.md)

### 模块定位

负责接收、存储和查询流水线构建过程中产生的日志数据。

### 核心功能

- 日志接收与存储
- 实时流式输出
- 多存储后端（ES/Lucene）
- 日志索引与查询

### 典型场景

- 实时查看构建日志
- 日志搜索与分析
- 日志下载与归档

---

## 五、质量红线模块 (Quality)

详见 [reference/5-quality-module.md](./reference/5-quality-module.md)

### 模块定位

负责在流水线执行过程中进行质量检查和拦截，确保只有符合质量标准的构建才能继续执行或发布。

### 核心功能

- 质量规则管理
- 指标配置与采集
- 准入准出控制点
- 红线拦截与放行

### 典型场景

- 代码扫描质量检查
- 测试覆盖率检查
- 安全漏洞拦截

---

## 六、开放接口模块 (OpenAPI)

详见 [reference/6-openapi-module.md](./reference/6-openapi-module.md)

### 模块定位

对外暴露的 API 网关服务，负责将内部微服务的能力通过标准化的 API 对外开放，供第三方系统集成调用。

### 核心功能

- API 网关路由
- 接口鉴权（AppCode/OAuth）
- 限流配置
- SDK 生成与文档

### 典型场景

- 第三方系统触发流水线
- CI/CD 平台集成
- 自动化脚本调用

---

## 跨模块协作：实战示例

以下示例展示通过 OpenAPI 创建凭证、触发流水线、查询质量红线结果的完整流程：

```kotlin
// 1. 通过 Ticket 模块创建 SSH 凭证
val credentialClient = client.get(ServiceCredentialResource::class)
val createResult = credentialClient.create(
    userId = "admin",
    projectId = "my-project",
    credential = CredentialCreate(
        credentialId = "my-ssh-key",
        credentialType = CredentialType.SSH_PRIVATEKEY,
        v1 = Base64.encode(sshPrivateKey),  // 私钥内容
        v2 = passphrase                      // 密码短语（可选）
    )
)
check(createResult.isOk()) { "凭证创建失败: ${createResult.message}" }

// 2. 通过 OpenAPI 触发流水线（使用刚创建的凭证）
val buildClient = client.get(ApigwBuildResourceV4::class)
val buildResult = buildClient.start(
    appCode = "my-app",
    apigwType = "apigw",
    userId = "admin",
    projectId = "my-project",
    pipelineId = "p-xxxx",
    values = mapOf("credentialId" to "my-ssh-key")
)
check(buildResult.isOk()) { "流水线触发失败: ${buildResult.message}" }
val buildId = buildResult.data?.id ?: error("构建 ID 为空")

// 3. 查询质量红线拦截结果
val qualityClient = client.get(ServiceQualityRuleResource::class)
val gateResult = qualityClient.getGateResult(
    projectId = "my-project",
    pipelineId = "p-xxxx",
    buildId = buildId
)
when {
    gateResult.data?.isPass == true -> println("质量红线检查通过")
    else -> {
        // 4. 红线拦截时通过 Notify 发送通知
        val notifyClient = client.get(ServiceNotifyResource::class)
        notifyClient.sendEmailNotify(
            EmailNotifyMessage(
                receivers = setOf("dev-team@example.com"),
                title = "质量红线拦截: 构建 $buildId",
                body = "流水线 p-xxxx 未通过质量检查，请查看详情。"
            )
        )
    }
}
```

---

## 开发工作流

### 新增凭证类型

1. 在 `api-ticket` 的 `CredentialType` 枚举中添加新类型
2. 创建对应的 `CredentialItem` 数据类定义字段映射
3. 在 `biz-ticket` 的 `CredentialService` 中实现加密存储逻辑
4. **验证**: 调用 `GET /api/ticket/credentials/{credentialId}` 确认凭证可正确创建和读取

### 添加通知渠道

1. 在 `NotifyType` 中注册新渠道标识
2. 实现 `AbstractSendNotifyService` 子类处理发送逻辑
3. 在通知模板配置中支持新渠道
4. **验证**: 发送测试通知，检查 `T_NOTIFY_MESSAGE` 表中记录状态为 `SUCCESS`

### 配置质量红线规则

1. 通过 `ServiceQualityIndicatorResource` 注册自定义指标
2. 创建规则绑定指标到流水线的准入/准出控制点
3. 配置拦截阈值和通知策略
4. **验证**: 执行流水线，确认 `GET /api/quality/rules/{ruleId}/intercept` 返回预期的拦截/放行结果

---

## 模块间交互场景

### 场景 1: 流水线执行完整流程

```
1. Process (启动流水线)
    ↓
2. Ticket (获取 Git 凭证) → 失败则中止并通知
    ↓
3. Dispatch (分配构建机)
    ↓
4. Environment (获取节点信息) → 无可用节点则排队等待
    ↓
5. Worker (执行构建任务)
    ↓
6. Log (上报构建日志)
    ↓
7. Quality (质量检查) → 红线拦截则触发通知
    ↓
8. Notify (发送结果通知)
```

### 场景 2: 第三方系统集成

```
第三方系统
    ↓
OpenAPI (接口鉴权 - AppCode 校验)
    ↓
Process (触发流水线)
    ↓
支撑模块 (Ticket/Env/Log/Quality/Notify)
    ↓
OpenAPI (返回结果)
```

---

## 相关 Skill

### 核心业务模块

- [process-module-architecture](../process-module-architecture/SKILL.md) - 流水线引擎
- [auth-module-architecture](../auth-module-architecture/SKILL.md) - 权限中心
- [project-module-architecture](../project-module-architecture/SKILL.md) - 项目管理
- [repository-module-architecture](../repository-module-architecture/SKILL.md) - 代码库
- [store-module-architecture](../store-module-architecture/SKILL.md) - 研发商店
- [artifactory-module-architecture](../artifactory-module-architecture/SKILL.md) - 制品库
- [dispatch-module-architecture](../dispatch-module-architecture/SKILL.md) - 构建调度
- [worker-module-architecture](../worker-module-architecture/SKILL.md) - 构建执行器
- [agent-module-architecture](../agent-module-architecture/SKILL.md) - 构建机

### 全局架构

- [00-bkci-global-architecture](../00-bkci-global-architecture/SKILL.md) - 全局架构指南
