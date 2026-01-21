---
name: environment-module-architecture
description: Environment 构建机环境模块架构指南，涵盖构建机节点管理、环境变量配置、节点状态监控、第三方构建机接入。当用户开发环境管理功能、配置构建机节点、处理环境变量或接入第三方构建机时使用。
---

# Environment 构建机环境模块架构指南

> **模块定位**: Environment 是 BK-CI 的构建机环境管理模块，负责管理构建节点（Node）、构建环境（Env）、第三方构建机（Agent）的注册、状态监控、分组等功能。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/environment/
├── api-environment/         # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/environment/
│       ├── api/                 # REST API 接口
│       │   └── thirdpartyagent/ # 第三方构建机接口
│       ├── constant/            # 常量定义
│       ├── exception/           # 异常定义
│       └── pojo/                # 数据对象
│           ├── enums/           # 枚举
│           ├── slave/           # 从属节点
│           └── thirdpartyagent/ # 第三方构建机
│
├── biz-environment/         # 业务逻辑层
├── model-environment/       # 数据模型层
└── boot-environment/        # Spring Boot 启动模块
```

### 1.2 核心概念

| 概念 | 说明 |
|------|------|
| **Node（节点）** | 构建机节点，可以是物理机、虚拟机或容器 |
| **Env（环境）** | 节点分组，一个环境包含多个节点 |
| **Agent** | 第三方构建机上运行的代理程序 |

## 二、核心枚举

### 2.1 节点类型

```kotlin
enum class NodeType(val type: String) {
    THIRDPARTY("thirdPartyAgentId"),  // 第三方构建机
    CMDB("cmdb"),                      // CMDB 导入
    DEVCLOUD("devcloud"),              // DevCloud
    OTHER("other"),                    // 其他
}
```

### 2.2 节点状态

```kotlin
enum class NodeStatus(val status: String) {
    NORMAL("NORMAL"),           // 正常
    ABNORMAL("ABNORMAL"),       // 异常
    DELETED("DELETED"),         // 已删除
    CREATING("CREATING"),       // 创建中
    RUNNING("RUNNING"),         // 运行中
    STARTING("STARTING"),       // 启动中
    STOPPING("STOPPING"),       // 停止中
    STOPPED("STOPPED"),         // 已停止
    RESTARTING("RESTARTING"),   // 重启中
    DELETING("DELETING"),       // 删除中
    BUILD_FAIL("BUILD_FAIL"),   // 构建失败
}
```

### 2.3 环境类型

```kotlin
enum class EnvType(val type: String) {
    DEV("DEV"),     // 开发环境
    TEST("TEST"),   // 测试环境
    BUILD("BUILD"), // 构建环境
}
```

### 2.4 操作系统类型

```kotlin
enum class OsType(val os: String) {
    LINUX("LINUX"),
    MACOS("MACOS"),
    WINDOWS("WINDOWS"),
}
```

## 三、核心数据库表

### 3.1 节点表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_NODE` | 节点信息表 | `NODE_ID`, `PROJECT_ID`, `NODE_IP`, `NODE_NAME`, `NODE_STATUS`, `NODE_TYPE` |
| `T_ENV` | 环境信息表 | `ENV_ID`, `PROJECT_ID`, `ENV_NAME`, `ENV_TYPE`, `ENV_VARS` |
| `T_ENV_NODE` | 环境-节点关联表 | `ENV_ID`, `NODE_ID`, `PROJECT_ID`, `ENABLE_NODE` |

### 3.2 第三方构建机表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_ENVIRONMENT_THIRDPARTY_AGENT` | 第三方构建机信息 | `ID`, `NODE_ID`, `PROJECT_ID`, `HOSTNAME`, `IP`, `OS`, `STATUS`, `SECRET_KEY` |
| `T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION` | 构建机操作记录 | `AGENT_ID`, `PROJECT_ID`, `ACTION`, `ACTION_TIME` |
| `T_ENVIRONMENT_AGENT_PIPELINE` | 构建机流水线关联 | `AGENT_ID`, `PROJECT_ID`, `PIPELINE` |

### 3.3 其他表

| 表名 | 说明 |
|------|------|
| `T_ENVIRONMENT_SLAVE_GATEWAY` | 从属网关配置 |
| `T_ENV_SHARE_PROJECT` | 环境共享项目 |
| `T_PROJECT_CONFIG` | 项目配置 |
| `T_NODE_TAG` | 节点标签 |

### 3.4 字段说明

> ⚠️ **重要**: `PROJECT_ID` 是 `T_PROJECT.english_name`

## 四、API 接口速查

### 4.1 用户接口

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserEnvironmentResource` | `/user/environment` | 用户环境管理 |
| `UserNodeResource` | `/user/node` | 用户节点管理 |
| `UserThirdPartyAgentResource` | `/user/thirdPartyAgent` | 用户第三方构建机管理 |
| `UserNodeTagResource` | `/user/node/tag` | 节点标签管理 |

### 4.2 服务间接口

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `ServiceEnvironmentResource` | `/service/environment` | 服务间环境查询 |
| `ServiceNodeResource` | `/service/node` | 服务间节点查询 |
| `ServiceThirdPartyAgentResource` | `/service/thirdPartyAgent` | 服务间构建机查询 |

### 4.3 构建机接口

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `BuildAgentThirdPartyAgentResource` | `/build/agent/thirdPartyAgent` | Agent 心跳上报 |
| `BuildAgentThirdPartyAgentUpgradeResource` | `/build/agent/upgrade` | Agent 升级 |

## 五、核心流程

### 5.1 第三方构建机安装流程

```
用户在界面创建构建机
    │
    ▼
UserThirdPartyAgentResource.generateLink()
    │
    ├─► 生成安装脚本和 Secret Key
    │
    └─► 返回安装命令
    
用户在目标机器执行安装脚本
    │
    ▼
Agent 启动并注册
    │
    ▼
BuildAgentThirdPartyAgentResource.agentStartup()
    │
    ├─► 验证 Secret Key
    ├─► 创建 Agent 记录
    └─► 返回 Agent ID
```

### 5.2 Agent 心跳流程

```
Agent 定期发送心跳
    │
    ▼
BuildAgentThirdPartyAgentResource.agentHeartbeat()
    │
    ├─► 更新 Agent 状态
    ├─► 检查是否有待执行任务
    └─► 返回心跳响应（包含任务信息）
```

### 5.3 环境创建流程

```
用户创建环境
    │
    ▼
UserEnvironmentResource.create()
    │
    ├─► 参数校验
    ├─► 创建环境记录
    └─► 关联节点（如有）
```

## 六、与其他模块的关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    Environment 模块关系                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────┐    ┌───────────┐                                 │
│  │  process  │    │ dispatch  │                                 │
│  │(流水线配置)│    │(构建调度)  │                                 │
│  └─────┬─────┘    └─────┬─────┘                                 │
│        │                │                                        │
│        └────────┬───────┘                                        │
│                 ▼                                                │
│         ┌───────────────┐                                        │
│         │  environment  │                                        │
│         └───────┬───────┘                                        │
│                 │                                                │
│         ┌───────┴───────┐                                        │
│         ▼               ▼                                        │
│  ┌───────────┐   ┌───────────┐                                  │
│  │  project  │   │   auth    │                                  │
│  └───────────┘   └───────────┘                                  │
│                                                                  │
│  被依赖：agent（构建机注册和心跳）                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 七、服务间调用示例

```kotlin
// Dispatch 模块获取环境下的 Agent 列表
client.get(ServiceThirdPartyAgentResource::class).getAgentsByEnvId(
    projectId = projectId,  // english_name
    envId = envId
)

// Process 模块获取节点信息
client.get(ServiceNodeResource::class).listByHashIds(
    userId = userId,
    projectId = projectId,
    nodeHashIds = nodeHashIds
)
```

## 八、常见问题

**Q: Agent 如何与服务端通信？**
A: Agent 通过 HTTP 心跳与 Environment 服务通信，定期上报状态并获取任务。

**Q: 环境和节点的关系？**
A: 一个环境可以包含多个节点，一个节点也可以属于多个环境。

**Q: 如何判断 Agent 是否在线？**
A: 检查 `T_ENVIRONMENT_THIRDPARTY_AGENT.STATUS` 字段和最近心跳时间。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
