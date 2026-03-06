---
name: 00-bkci-global-architecture
description: BK-CI 全局架构指南，以流水线为核心的模块协作全景图，涵盖完整执行流程、模块依赖关系、数据流向、核心概念。当用户需要理解系统架构、进行跨模块开发、了解模块间协作或规划架构设计时优先阅读。
# 结构化元数据（支持智能压缩和渐进式加载）
core_modules:
  - Process (流水线核心)
  - Auth (权限认证)
  - Project (项目管理)
  - Store (研发商店)
  - Dispatch (构建调度)
related_skills:
  - process-module-architecture
  - auth-module-architecture
  - dispatch-module-architecture
  - worker-module-architecture
  - agent-module-architecture
token_estimate: 45000
---

# BK-CI 全局架构指南

<!-- ═══════════════════════════════════════════════════════════════════════════
     📚 Skill 导航索引（放在最前面 - 快速定位相关 Skill）
     ═══════════════════════════════════════════════════════════════════════════ -->

## 📚 Skill 导航索引

> **说明**: 本文档是全局架构总览，按需深入阅读具体模块的 Skill。

### 🗂️ 按类别查找 (31 个 Skill)

#### 1️⃣ 全局架构 (1)
- **`00-bkci-global-architecture`** (本文档) - 系统全局架构、模块协作、执行流程

#### 2️⃣ 通用技术实践 (3)
- **`common-technical-practices`** - 框架级实践：AOP、分布式锁、重试、监控、定时任务、审计日志
- **`microservice-infrastructure`** - 微服务基础：条件配置、事件驱动、服务调用、国际化、日志
- **`utility-components`** - 工具级组件：JWT、表达式解析、线程池、责任链

#### 3️⃣ 后端开发 (3)
- **`backend-microservice-development`** - 后端微服务开发规范、Controller/Service/Dao 模式
- **`api-interface-design`** - RESTful API 设计、接口规范
- **`unit-testing`** - 单元测试编写规范

#### 4️⃣ 前端开发 (1)
- **`frontend-vue-development`** - Vue 前端开发规范、组件设计

#### 5️⃣ 数据库 (1)
- **`database-design`** - 数据库表设计、字段规范、索引优化、DDL 脚本管理

#### 6️⃣ Pipeline 流水线系列 (4)
- **`pipeline-model-architecture`** - Pipeline 模型架构：Stage/Container/Element 结构
- **`pipeline-variable-management`** - 流水线变量管理：变量生命周期、字段扩展
- **`pipeline-template-module`** - 流水线模板模块：模板定义、实例化、版本管理
- **`pipeline-plugin-development`** - 流水线插件开发：插件规范、任务执行

#### 7️⃣ 核心模块架构 (9)
- **`process-module-architecture`** - Process 流水线模块（核心）：API/Service/Engine/Dao 四层架构
- **`auth-module-architecture`** - Auth 权限认证模块：RBAC、权限校验
- **`project-module-architecture`** - Project 项目管理模块：项目创建、配置、成员管理
- **`repository-module-architecture`** - Repository 代码库模块：Git/SVN 集成、Webhook
- **`store-module-architecture`** - Store 研发商店模块：插件/模板上架、版本管理
- **`artifactory-module-architecture`** - Artifactory 制品库模块：制品存储、版本管理
- **`dispatch-module-architecture`** - Dispatch 构建调度模块：构建机分配、调度策略
- **`worker-module-architecture`** - Worker 任务执行器模块：任务领取、插件执行
- **`agent-module-architecture`** - Agent 构建机代理模块 (Go)：进程管理、日志上报

#### 8️⃣ 支撑模块架构 (1)
- **`supporting-modules-architecture`** - 支撑模块总览：
  - Ticket (凭证管理)
  - Environment (构建机环境)
  - Notify (通知服务)
  - Log (构建日志)
  - Quality (质量红线)
  - OpenAPI (开放接口)

#### 9️⃣ 设计模式 (1)
- **`design-patterns`** - 设计模式实践：工厂、策略、观察者、责任链等

#### 🔟 其他工具与规范 (7)
- **`git-commit-specification`** - Git 提交规范：Commit Message 格式
- **`managing-devops-pipeline`** - 蓝盾流水线管理：MCP 工具使用指南
- **`business-knowledge-workflow`** - 业务知识获取流程：如何获取领域知识
- **`permission-model-change-guide`** - 权限模型变更指南：v3 → v4 迁移
- **`go-agent-development`** - Go Agent 开发规范：Go 编码规范、构建机开发
- **`yaml-pipeline-transfer`** - YAML 流水线转换：v2.0 → v3.0 迁移
- **`skill-writer`** - Skill 编写指南：如何编写高质量 Skill

---

### 🎯 按场景快速查找

| 场景 | 涉及 Skill (按优先级排序) |
|------|---------------------------|
| **新增流水线功能** | `pipeline-model-architecture`, `process-module-architecture`, `pipeline-variable-management`, `microservice-infrastructure` |
| **开发新插件** | `store-module-architecture`, `worker-module-architecture`, `pipeline-plugin-development` |
| **修改权限逻辑** | `auth-module-architecture`, `common-technical-practices` |
| **优化构建调度** | `dispatch-module-architecture`, `agent-module-architecture`, `supporting-modules-architecture` |
| **数据库表变更** | `database-design` |
| **添加 API 接口** | `api-interface-design`, `backend-microservice-development` |
| **前端页面开发** | `frontend-vue-development` |
| **流水线变量扩展** | `pipeline-variable-management`, `utility-components` (表达式解析) |
| **实现分布式锁** | `common-technical-practices` |
| **添加通知功能** | `supporting-modules-architecture` (Notify 模块) |
| **凭证管理** | `supporting-modules-architecture` (Ticket 模块) |
| **质量红线** | `supporting-modules-architecture` (Quality 模块) |
| **日志查询** | `supporting-modules-architecture` (Log 模块) |
| **OAuth2 认证** | `auth-module-architecture`, `utility-components` (JWT) |

---

### 🧭 学习路径推荐

#### 新手入门路径
```
1. 00-bkci-global-architecture (本文档) - 建立全局视图
   ↓
2. pipeline-model-architecture - 理解流水线模型
   ↓
3. process-module-architecture - 深入核心模块
   ↓
4. backend-microservice-development - 掌握开发规范
```

#### 进阶开发路径
```
根据开发任务选择：
- 插件开发 → store + worker + pipeline-plugin-development
- 权限功能 → auth + common-technical-practices
- 调度优化 → dispatch + agent + supporting-modules-architecture
- 变量扩展 → pipeline-variable-management + utility-components
```

---

<!-- ═══════════════════════════════════════════════════════════════════════════
     🚀 快速参考区（放在最前面 - 解决 Lost-in-Middle 问题）
     ═══════════════════════════════════════════════════════════════════════════ -->

## Quick Reference

### 系统分层（5 层）

```
用户层 → 网关层 → 微服务层 → 构建机层 → 资源层
```

### 核心模块速查

| 模块 | 职责 | 深入阅读 |
|------|------|----------|
| **Process** | 流水线编排与执行（核心） | `process-module-architecture` |
| **Auth** | 权限认证 RBAC | `auth-module-architecture` |
| **Dispatch** | 构建机分配调度 | `dispatch-module-architecture` |
| **Store** | 研发商店/插件管理 | `store-module-architecture` |
| **Worker** | 任务执行器 | `worker-module-architecture` |
| **Agent** | 构建机代理 (Go) | `agent-module-architecture` |

### 流水线执行核心流程

```
触发 → Process引擎 → Dispatch分配 → Agent领取 → Worker执行 → 状态回传
```

### 关键代码入口

| 功能 | 入口类 |
|------|--------|
| 手动构建 | `PipelineBuildFacadeService.buildManualStartup()` |
| 引擎调度 | `StageControl` / `ContainerControl` / `TaskControl` |
| Worker 主循环 | `Runner.loopPickup()` |
| 任务完成处理 | `PipelineBuildTaskService.finishTask()` |

---

## When to Use ✅

- 首次接触 BK-CI 项目，需要建立全局视图
- 进行**跨模块开发**，需要理解模块间协作
- 调试复杂问题，需要追踪完整调用链路
- 规划架构设计或重构方案
- 不确定某个功能应该在哪个模块实现

## When NOT to Use ❌

- 只修改单个模块内部逻辑（直接阅读对应模块 Skill）
- 简单的 Bug 修复（不涉及跨模块调用）
- 前端 UI 调整（阅读 `frontend-vue-development`）
- 数据库表变更（阅读 `database-design`）

---

## 一、系统分层架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           用户层 (User Layer)                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Web 前端   │  │  OpenAPI    │  │  Webhook    │  │  第三方系统集成     │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼────────────────┼────────────────────┼────────────┘
          ▼                ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         网关层 (Gateway Layer)                               │
│                    OpenResty (Nginx + Lua)                                   │
│  • 路由转发 • 身份认证 • 限流熔断 • 服务发现(Consul)                         │
└─────────────────────────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       微服务层 (Microservice Layer)                          │
│  核心: Project | Process | Auth | Store | Repository                         │
│  调度: Dispatch | Environment | Ticket                                       │
│  支撑: Artifactory | Log | Notify | Quality | Metrics                        │
│  开放: OpenAPI | WebSocket                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        构建机层 (Build Machine Layer)                        │
│     Agent (Go) ──▶ Worker (Kotlin)                                           │
│     进程守护        任务执行、插件运行、日志上报                              │
└─────────────────────────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        资源层 (Resource Layer)                               │
│     MySQL | Redis | RabbitMQ | ElasticSearch | 文件存储                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 模块职责速查表

| 模块 | 核心职责 | 关键能力 |
|------|----------|----------|
| **Project** | 项目管理 | 项目创建、成员管理、服务开通、路由分片 |
| **Process** | 流水线编排与执行 | 模型定义、构建调度、事件驱动、状态管理 |
| **Auth** | 权限认证 | RBAC、资源授权、用户组、OAuth2 |
| **Store** | 研发商店 | 插件管理、模板市场、版本发布、统计分析 |
| **Repository** | 代码库管理 | Git/SVN/P4 对接、Webhook、PAC |
| **Dispatch** | 构建调度 | 构建机分配、Docker 调度、配额管理 |
| **Environment** | 构建机环境 | 节点管理、Agent 安装、环境变量 |
| **Ticket** | 凭证管理 | 密码/SSH/Token 存储、加密解密 |
| **Artifactory** | 制品库 | 文件存储、版本管理、跨项目共享 |
| **Log** | 日志服务 | 构建日志收集、存储、检索 |
| **Notify** | 通知服务 | 邮件/企微/RTX 通知 |
| **Quality** | 质量红线 | 指标定义、准入准出、拦截规则 |
| **Agent** | 构建机代理 | 进程管理、任务调度、自动升级 |
| **Worker** | 任务执行器 | 插件执行、脚本运行、日志上报 |

---

## 二、流水线执行全流程（核心）

> **这是 BK-CI 最核心的流程，涉及几乎所有模块的协作。**

### 2.1 执行流程总览图

```
用户触发
    │
    ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ PipelineBuild   │────▶│ PipelineBuild   │────▶│ PipelineRuntime │
│ FacadeService   │     │ Service         │     │ Service         │
│ (权限校验)      │     │ (拦截器链)      │     │ (创建记录)      │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │ PipelineBuildStart  │
                                              │ Event → RabbitMQ    │
                                              └──────────┬──────────┘
                                                         │
    ┌────────────────────────────────────────────────────┘
    ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ BuildStart      │────▶│ StageControl    │────▶│ Container       │
│ Control         │     │ (责任链)        │     │ Control         │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                              ┌───────────────────────────┤
                              ▼                           ▼
                   ┌─────────────────┐         ┌─────────────────┐
                   │ Dispatch        │         │ TaskControl     │
                   │ (构建机分配)    │         │ (引擎侧Task)    │
                   └────────┬────────┘         └─────────────────┘
                            ▼
                   ┌─────────────────┐
                   │ Agent (Go)      │
                   │ (领取任务)      │
                   └────────┬────────┘
                            ▼
                   ┌─────────────────┐
                   │ Worker (Kotlin) │
                   │ (执行Task)      │
                   └────────┬────────┘
                            │ completeTask API
                            ▼
                   ┌─────────────────┐
                   │ 状态逐层回传    │
                   │ Task→Job→Stage  │
                   │ →Pipeline       │
                   └────────┬────────┘
                            ▼
                   ┌─────────────────┐
                   │ BuildEndControl │
                   │ (完成处理)      │
                   └────────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Notify    │     │   Metrics   │     │  WebSocket  │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 2.2 四个阶段概要

| 阶段 | 核心动作 | 深入阅读 |
|---|---|---|
| **触发** | 手动/定时/Webhook/OpenAPI → `PipelineBuildFacadeService.buildManualStartup()` | `process-module-architecture` |
| **引擎调度** | Stage → Job → Task 责任链编排 | `3-engine-control.md` 第三节 |
| **任务执行** | Agent(Go) 领取构建 → 拉起 Worker(Kotlin) → 循环 claimTask/completeTask | `agent-module-architecture` 3.2~3.3 节, `worker-module-architecture` Runner 节 |
| **状态回传** | completeTask → `PipelineBuildContainerEvent` → 逐层收敛到 BuildEnd | `3-engine-control.md` 3.4.2 节 |

> Task 的 5 种角色分类、引擎与 Worker 的事件驱动协作机制，详见 `3-engine-control.md` 3.4 节。

---

## 三、引擎-调度-构建机 完整交互流程

> 本节将 Process 引擎、Dispatch 调度、Agent/Worker、BuildLess 容器池的完整交互串联起来，
> 覆盖从"用户触发构建"到"Job 完成"的全链路。

### 3.1 构建机环境（第三方/DevCloud/Docker）全链路时序

```
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  用户/   │  │ Process  │  │ RabbitMQ │  │ Dispatch │  │  Agent   │  │  Worker  │
│  触发器  │  │  引擎    │  │          │  │  服务    │  │  (Go)    │  │ (Kotlin) │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │              │              │              │              │              │
  ① │─ 触发构建 ──>│              │              │              │              │
     │              │              │              │              │              │
     │         ┌────┴────────────────────────────────────────────────────────┐  │
     │         │ ② 引擎编排阶段                                              │  │
     │         │ BuildStartControl → StageControl → ContainerControl        │  │
     │         │ → StartActionTaskContainerCmd 选出 startVM-{seq} 任务       │  │
     │         │ → TaskControl 执行 DispatchVMStartupTaskAtom               │  │
     │         └────┬────────────────────────────────────────────────────────┘  │
     │              │              │              │              │              │
     │              │── ③ dispatch(PipelineAgentStartupEvent) ──>│              │
     │              │              │              │              │              │
     │              │              │── ④ 消费 ──>│              │              │
     │              │              │   事件      │              │              │
     │              │              │              │              │              │
     │              │              │              │─ ⑤ 选择构建资源 ─>          │
     │              │              │              │  (IP/容器/VM)│              │
     │              │              │              │              │              │
     │              │              │              │── ⑥ 启动 ──>│              │
     │              │              │              │  Worker 进程 │              │
     │              │              │              │              │──── ⑦ ────>│
     │              │              │              │              │  拉起Worker │
     │              │              │              │              │              │
     │         ┌────────────────────────────────────────────────────────────────┤
     │         │ ⑧ Worker 启动流程                                              │
     │         │ EngineService.setStarted() → 上报启动，获取 BuildVariables     │
     │         │ 对应服务端：EngineVMBuildService.buildVMStarted()              │
     │         │   → 校验构建状态                                               │
     │         │   → setStartUpVMStatus(SUCCEED) 改 startVM 任务为成功          │
     │         │   → 发送 PipelineBuildContainerEvent 唤醒引擎                  │
     │         │   → 启动心跳监听                                               │
     │         │   → 返回 BuildVariables（变量、超时、环境等）                    │
     │         └────────────────────────────────────────────────────────────────┤
     │              │              │              │              │              │
     │         ┌────┴────────────────────────────────────────────────────────┐  │
     │         │ ⑨ 引擎被唤醒，编排第一个用户插件任务                          │  │
     │         │ ContainerControl → StartActionTaskContainerCmd              │  │
     │         │   → 选出第一个插件任务                                       │  │
     │         │   → updateTaskStatus(QUEUE_CACHE)                          │  │
     │         │   → TaskControl 发现 taskAtom 为空，直接 return             │  │
     │         │   → 引擎停住，等待回调                                       │  │
     │         └────┬────────────────────────────────────────────────────────┘  │
     │              │              │              │              │              │
     │              │              │              │              │         ┌────┤
     │              │              │              │              │         │ ⑩ │
     │              │              │              │              │         │循环│
     │              │<─────────── claimTask（轮询）────────────────────────│领取│
     │              │              │              │              │         │任务│
     │              │──── 返回 BuildTask(DO) ────────────────────────────>│    │
     │              │              │              │              │         │    │
     │              │              │              │              │         │执行│
     │              │              │              │              │         │插件│
     │              │              │              │              │         │    │
     │              │<─────────── completeTask(result) ──────────────────│    │
     │              │              │              │              │         │    │
     │         ┌────┴────────────────────────────────────────────────┐    │    │
     │         │ ⑪ 引擎被唤醒                                        │    │    │
     │         │ completeClaimBuildTask()                             │    │    │
     │         │   → updateTaskStatus(SUCCEED/FAILED)                │    │    │
     │         │   → dispatch(PipelineBuildContainerEvent)           │    │    │
     │         │   → 引擎编排下一个任务 → QUEUE_CACHE → 再次停住      │    │    │
     │         └────┬────────────────────────────────────────────────┘    │    │
     │              │              │              │              │         │    │
     │              │   ... 重复 ⑩⑪ 直到所有插件任务完成 ...              │    │
     │              │              │              │              │         └────┤
     │              │              │              │              │              │
     │         ┌────┴────────────────────────────────────────────────────────┐  │
     │         │ ⑫ Job 结束阶段                                              │  │
     │         │ Worker 领到 end-{seq} 任务 → 返回 BuildTaskStatus.END       │  │
     │         │ Worker 调用 EngineService.endBuild() → finishWorker()       │  │
     │         │ 引擎执行 stopVM-{seq}                                       │  │
     │         │   → DispatchVMShutdownTaskAtom                              │  │
     │         │   → 发送 PipelineAgentShutdownEvent 给 Dispatch 回收资源     │  │
     │         │ Job 完成 → Stage 完成检查 → 下一个 Stage 或 BuildEnd        │  │
     │         └─────────────────────────────────────────────────────────────┘  │
     │              │              │              │              │              │
```

### 3.2 无编译环境（BuildLess）全链路时序

BuildLess 与普通构建机的关键差异在于**资源准备阶段**：
不走 Agent，改走容器池 + Redis 任务队列认领。

```
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Process  │  │ RabbitMQ │  │ Dispatch │  │ BuildLess│  │  Worker  │
│  引擎    │  │          │  │ (docker) │  │ 容器池   │  │ (容器内) │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │              │              │              │              │
  ① │ startVM 任务执行             │              │              │
     │ DispatchBuildLessDockerStartupTaskAtom     │              │
     │ 返回 CALL_WAITING（注意：不是 RUNNING）     │              │
     │              │              │              │              │
  ② │── dispatch(PipelineBuildLessStartupEvent) ─>│              │
     │              │              │              │              │
     │              │── ③ 消费 ──>│              │              │
     │              │              │              │              │
     │              │              │─ ④ 配额检查 ─>              │
     │              │              │   checkAndAddRunningJob      │
     │              │              │              │              │
     │              │              │─ ⑤ HTTP ───>│              │
     │              │              │ start()     │              │
     │              │              │              │              │
     │              │              │              │─ ⑥ ────────>│
     │              │              │    ContainerPoolExecutor     │
     │              │              │    .execute()                │
     │              │              │      │                       │
     │              │              │      ├── 拒绝策略检查         │
     │              │              │      │  (ABORT/FOLLOW/JUMP)  │
     │              │              │      │                       │
     │              │              │      └── LPUSH 任务入队       │
     │              │              │         Redis ready_task     │
     │              │              │              │              │
     │              │              │              │  ⑦ 容器轮询   │
     │              │              │              │<── RPOP ────│
     │              │              │              │  claimTask  │
     │              │              │              │              │
     │              │              │              │── 返回 ────>│
     │              │              │              │ BuildLessTask│
     │              │              │              │              │
     │              │              │              │         ┌────┤
     │              │              │              │         │ ⑧ │
     │<──────────── setStarted / buildVMStarted ──────────│启动│
     │              │              │              │         │    │
     │── 返回 BuildVariables ────────────────────────────>│    │
     │              │              │              │         │    │
     │<──────────── claimTask ────────────────────────────│领取│
     │── 返回 BuildTask(DO) ─────────────────────────────>│执行│
     │              │              │              │         │    │
     │<──────────── completeTask ─────────────────────────│    │
     │  唤醒引擎，编排下一个任务                            │    │
     │              │              │              │         │    │
     │   ... 重复领取/完成直到所有插件任务结束 ...           │    │
     │              │              │              │         └────┤
     │              │              │              │              │
  ⑨ │ stopVM 任务执行              │              │              │
     │ DispatchBuildLessDockerShutdownTaskAtom    │              │
     │── dispatch(PipelineBuildLessShutdownEvent)─>              │
     │              │              │              │              │
     │              │              │─ 回收配额 ──>│              │
     │              │              │ removeRunningJob             │
     │              │              │              │─ 销毁容器 ──>│
     │              │              │              │              │
```

### 3.3 两种环境的关键差异对比

| 对比项 | 构建机环境 | 无编译环境（BuildLess） |
|---|---|---|
| **启动原子** | `DispatchVMStartupTaskAtom` | `DispatchBuildLessDockerStartupTaskAtom` |
| **启动事件** | `PipelineAgentStartupEvent` | `PipelineBuildLessStartupEvent` |
| **容器参数** | `VMBuildContainer` | `NormalContainer` |
| **启动返回值** | `BuildStatus.RUNNING` | `BuildStatus.CALL_WAITING` |
| **资源准备** | Dispatch 选 Agent/VM → Agent 拉起 Worker | Dispatch → BuildLess 容器池 → Redis 队列认领 |
| **Worker 来源** | Agent(Go) 拉起 Worker JAR 进程 | 容器内预装 Worker，从 Redis 认领任务后启动 |
| **任务认领** | Worker 直接 `claimTask` 轮询 Process | 容器先从 BuildLess Redis 队列 `RPOP` 认领构建，再 `claimTask` 领插件 |
| **关闭事件** | `PipelineAgentShutdownEvent` | `PipelineBuildLessShutdownEvent` |
| **资源回收** | Dispatch 通知 Agent 停止/销毁容器 | 容器即用即销 + `removeRunningJob` 回收配额 |
| **启动耗时** | 5~60 秒（取决于 VM/容器类型） | < 1 秒（容器池预热） |

> Task 的 5 种角色分类和 TaskControl 分流全景，详见 `3-engine-control.md` 3.4.1 节。
> Worker 主循环（Runner.kt）的完整通信链路，详见 `worker-module-architecture` Runner 节。

### 3.4 状态流转总览

一个 Job 内 task 的完整状态流转：

```
                    引擎编排                Worker 领取             Worker 完成
                       │                       │                       │
 ┌────────┐    ┌───────┴──────┐    ┌───────────┴─────────┐    ┌───────┴───────┐
 │ QUEUE  │───>│ QUEUE_CACHE  │───>│     RUNNING         │───>│SUCCEED/FAILED │
 │(初始化)│    │(引擎选出待执行)│    │(Worker claimBuildTask)│   │(completeTask) │
 └────────┘    └──────────────┘    └─────────────────────┘    └───────┬───────┘
                                                                      │
                                                                      ▼
                                                          dispatch(ContainerEvent)
                                                                      │
                                                                      ▼
                                                            引擎唤醒，选下一个任务
                                                          重复上述流程直到 end-{seq}
```

**特殊状态**:

| 状态 | 含义 | 触发场景 |
|---|---|---|
| `QUEUE` | 初始状态，任务还未被引擎选中 | 构建记录刚创建 |
| `QUEUE_CACHE` | 引擎已选出，等待 Worker 领取 | `StartActionTaskContainerCmd` |
| `RUNNING` | Worker 已领取，正在执行 | `claimBuildTask()` |
| `SUCCEED` | 执行成功 | Worker `completeTask` |
| `FAILED` | 执行失败 | Worker `completeTask` |
| `CANCELED` | 用户取消 | 心跳返回取消信号 |
| `UNEXEC` | 未执行（被跳过） | terminate 或条件不满足 |
| `CALL_WAITING` | 等待外部回调 | BuildLess startVM 返回 |

### 3.5 关键代码入口速查

| 阶段 | 代码入口 | 模块 |
|---|---|---|
| 触发构建 | `PipelineBuildFacadeService.buildManualStartup()` | process/biz-process |
| 引擎启动 | `BuildStartControl.handle()` | process/biz-engine |
| Stage 编排 | `StageControl.handle()` → 命令链 | process/biz-engine |
| Job 编排 | `ContainerControl.handle()` → 命令链 | process/biz-engine |
| 选出待执行 Task | `StartActionTaskContainerCmd.findNeedToRunTask()` | process/biz-engine |
| Task 分流 | `TaskControl.handle()` → `runByVmTask()` | process/biz-engine |
| 构建机调度（启动） | `DispatchVMStartupTaskAtom.execute()` | process/biz-engine |
| BuildLess 调度（启动） | `DispatchBuildLessDockerStartupTaskAtom.startUpDocker()` | process/biz-engine |
| Dispatch 监听 | `DockerVMListener` / `ThirdPartyBuildListener` / `BuildLessListener` | dispatch |
| BuildLess 任务入队 | `ContainerPoolExecutor.execute()` | buildless |
| BuildLess 任务认领 | `BuildLessTaskService.claimBuildLessTask()` | buildless |
| Worker 启动上报 | `EngineVMBuildService.buildVMStarted()` | process/biz-base |
| Worker 领取任务 | `EngineVMBuildService.buildClaimTask()` → `claim()` | process/biz-base |
| Worker 完成任务 | `EngineVMBuildService.buildCompleteTask()` | process/biz-base |
| 引擎被唤醒 | `PipelineRuntimeService.completeClaimBuildTask()` | process/biz-base |
| Worker 主循环 | `Runner.loopPickup()` | worker/worker-common |
| Agent 领取构建 | `doAsk()` → `job.DoBuild()` | agent (Go) |
| 构建结束 | `BuildEndControl.handle()` | process/biz-engine |

---

## 四、服务依赖关系

```
                              ┌──────────┐
                              │ Project  │ ◀──── 所有服务的基础依赖
                              └────┬─────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
    ┌──────────┐             ┌──────────┐             ┌──────────┐
    │   Auth   │◀────────────│ Process  │────────────▶│  Store   │
    └────┬─────┘             └────┬─────┘             └──────────┘
         │           ┌────────────┼────────────┐
         │           ▼            ▼            ▼
         │     ┌──────────┐ ┌──────────┐ ┌──────────┐
         │     │Repository│ │ Dispatch │ │  Ticket  │
         │     └──────────┘ └────┬─────┘ └──────────┘
         │                       │
         │                       ▼
         │                ┌──────────┐
         │                │Environment│
         │                └────┬─────┘
         │                     ▼
         │              ┌────────────┐
         │              │Agent/Worker│
         │              └─────┬──────┘
         │                    ▼
         │     ┌──────────────────────────────────┐
         │     │  Log | Artifactory | Notify     │
         │     └──────────────────────────────────┘
         │                    │
         └───────────────────▶│
                              ▼
                       ┌──────────────┐
                       │   Metrics    │
                       └──────────────┘
```

---

## 五、核心事件类型

| 事件类型 | 触发时机 | 处理器 |
|----------|----------|--------|
| `PipelineBuildStartEvent` | 构建启动 | StageControl |
| `PipelineBuildStageEvent` | Stage 状态变更 | StageControl |
| `PipelineBuildContainerEvent` | Job 状态变更 | ContainerControl |
| `PipelineBuildTaskFinishEvent` | 任务完成 | TaskControl |
| `PipelineBuildFinishEvent` | 构建完成 | BuildFinishListener |
| `PipelineBuildCancelEvent` | 构建取消 | CancelControl |
| `PipelineAgentStartupEvent` | 构建机启动 | DispatchListener |

---

## 六、技术栈

| 层次 | 技术 |
|------|------|
| **后端** | Kotlin/Java, Spring Boot 3, Gradle |
| **数据库** | MySQL + JOOQ |
| **缓存/锁** | Redis |
| **消息队列** | RabbitMQ |
| **日志检索** | ElasticSearch |
| **服务发现** | Consul |
| **前端** | Vue 2.7, Vuex, bk-magic-vue |
| **Agent** | Go 1.19+ |
| **Worker** | Kotlin (JVM) |

---

## 七、深入阅读导航

以下内容已在专属 Skill 中详细覆盖，不在本文档重复：

| 主题 | 去哪里看 |
|---|---|
| Task 的 5 种角色分类 + TaskControl 分流 | `3-engine-control.md` 3.4.1 节 |
| Worker 执行插件的生命周期（事件驱动） | `3-engine-control.md` 3.4.2 节 |
| Worker 主循环 + API 通信 | `worker-module-architecture` Runner 节 |
| Agent 领取构建 + Ask 模式 | `agent-module-architecture` 3.2~3.3 节 |
| 流水线模型结构（Model/Stage/Container/Element） | `pipeline-model-architecture` |
| 数据库核心表（92 张表详解） | `process-module-architecture/reference/4-dao-database.md` |
| BuildLess 容器池管理 | `dispatch-module-architecture/reference/buildless.md` |

---

## Checklist

开始跨模块开发前，请确认：

- [ ] **已理解流水线执行流程**：触发 → 调度 → 执行 → 回传
- [ ] **已确定涉及的模块**：Process? Dispatch? Store?
- [ ] **已阅读相关模块 Skill**：如 `process-module-architecture`
- [ ] **已了解事件驱动机制**：通过 RabbitMQ 异步通信
- [ ] **已确认数据流向**：哪些表会被读写？

### 核心理解要点

1. **Process 是核心** - 流水线编排、构建调度、状态管理都在这里
2. **事件驱动** - 通过 RabbitMQ 实现模块间解耦和异步处理
3. **双层构建** - Agent(Go) 负责进程管理，Worker(Kotlin) 负责任务执行
4. **Worker Pull 模式** - Worker 主动 `claimTask` 领取任务
5. **非阻塞等待** - 引擎发出调度请求后不阻塞，通过事件回调继续
