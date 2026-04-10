---
name: supporting-modules-architecture
description: BK-CI 支撑模块架构指南，涵盖凭证管理(Ticket)、构建机环境(Environment)、通知服务(Notify)、构建日志(Log)、质量红线(Quality)、开放接口(OpenAPI)等支撑性服务模块。当用户开发这些模块功能或需要理解支撑服务架构时使用。
related_skills:
  - 00-bkci-global-architecture
  - process-module-architecture
token_estimate: 15000
---

# 支撑模块架构指南

## Skill 概述

本 Skill 涵盖了 BK-CI 的 **6 大支撑模块**，这些模块为核心业务提供支撑性服务，不直接参与流水线核心执行逻辑，但提供凭证、环境、通知、日志、质量检查、API 开放等关键能力。

### 模块列表

| 模块 | 说明 | 文档 |
|------|------|------|
| **Ticket** | 凭证管理模块 | [1-ticket-module.md](./reference/1-ticket-module.md) |
| **Environment** | 构建机环境模块 | [2-environment-module.md](./reference/2-environment-module.md) |
| **Notify** | 通知服务模块 | [3-notify-module.md](./reference/3-notify-module.md) |
| **Log** | 构建日志模块 | [4-log-module.md](./reference/4-log-module.md) |
| **Quality** | 质量红线模块 | [5-quality-module.md](./reference/5-quality-module.md) |
| **OpenAPI** | 开放接口模块 | [6-openapi-module.md](./reference/6-openapi-module.md) |

---

## 核心业务模块 vs 支撑模块

### 模块分类

```
BK-CI 模块架构
├── 核心业务模块 (直接参与流水线执行)
│   ├── Process (流水线引擎)
│   ├── Auth (权限中心)
│   ├── Project (项目管理)
│   ├── Repository (代码库)
│   ├── Store (研发商店)
│   ├── Artifactory (制品库)
│   ├── Dispatch (构建调度)
│   ├── Worker (构建执行器)
│   └── Agent (构建机)
│
└── 支撑服务模块 (提供支撑能力)
    ├── Ticket (凭证管理) ⭐
    ├── Environment (环境管理) ⭐
    ├── Notify (通知服务) ⭐
    ├── Log (日志服务) ⭐
    ├── Quality (质量检查) ⭐
    └── OpenAPI (API 网关) ⭐
```

### 定位对比

| 维度 | 核心业务模块 | 支撑服务模块 |
|------|-------------|-------------|
| **职责** | 流水线编排、执行、调度、存储 | 提供凭证、环境、通知、日志、质量、API 能力 |
| **耦合度** | 强耦合，相互依赖 | 松耦合，被动提供服务 |
| **调用方向** | 相互调用，协同执行 | 被核心模块调用 |
| **复杂度** | 高 (流程复杂) | 中低 (功能单一) |
| **独立性** | 低 (需协作) | 高 (独立服务) |

---

## 支撑模块全景视图

### 协作关系

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

## 模块间协作关系

### 典型交互场景

#### 场景 1: 流水线执行完整流程

```
1. Process (启动流水线)
    ↓
2. Ticket (获取 Git 凭证)
    ↓
3. Dispatch (分配构建机)
    ↓
4. Environment (获取节点信息)
    ↓
5. Worker (执行构建任务)
    ↓
6. Log (上报构建日志)
    ↓
7. Quality (质量检查)
    ↓ (失败)
8. Notify (发送失败通知)
```

#### 场景 2: 第三方系统集成

```
第三方系统
    ↓
OpenAPI (接口鉴权)
    ↓
Process (触发流水线)
    ↓
支撑模块 (Ticket/Env/Log/Quality/Notify)
    ↓
OpenAPI (返回结果)
```

---

## 使用场景决策树

```
用户需求
    ↓
需要开发/理解哪个模块？
    ├─ 凭证管理 → supporting-modules (reference/1-ticket)
    ├─ 构建机环境 → supporting-modules (reference/2-environment)
    ├─ 通知服务 → supporting-modules (reference/3-notify)
    ├─ 构建日志 → supporting-modules (reference/4-log)
    ├─ 质量红线 → supporting-modules (reference/5-quality)
    ├─ 开放 API → supporting-modules (reference/6-openapi)
    └─ 核心业务 → 查找对应核心模块 Skill
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

---

## Quick Reference

| 需求 | 模块 | 参考文档 |
|------|------|----------|
| 添加新凭证类型 | Ticket | reference/1-ticket-module.md |
| 管理构建机节点 | Environment | reference/2-environment-module.md |
| 添加通知渠道 | Notify | reference/3-notify-module.md |
| 实现日志存储 | Log | reference/4-log-module.md |
| 配置质量规则 | Quality | reference/5-quality-module.md |
| 开放新 API | OpenAPI | reference/6-openapi-module.md |
