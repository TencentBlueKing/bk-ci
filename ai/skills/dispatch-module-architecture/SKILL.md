---
name: dispatch-module-architecture
description: Dispatch 构建调度模块架构指南，涵盖构建机调度策略、资源分配、队列管理、Docker/K8s 调度、Agent 选择。当用户开发调度功能、配置调度策略、处理资源分配或实现新调度类型时使用。
---

# Dispatch 构建调度模块架构指南

> **模块定位**: Dispatch 是 BK-CI 的构建调度模块，负责接收流水线的构建任务，将任务分发到合适的构建机（第三方构建机、Docker 容器、Kubernetes Pod）上执行。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/dispatch/
├── api-dispatch/                # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/dispatch/
│       ├── api/                     # REST API 接口
│       ├── constants/               # 常量定义
│       └── pojo/                    # 数据对象
│           ├── enums/               # 枚举
│           ├── redis/               # Redis 数据结构
│           └── thirdpartyagent/     # 第三方构建机相关
│
├── api-dispatch-docker/         # Docker 调度 API
├── api-dispatch-kubernetes/     # Kubernetes 调度 API
│
├── biz-dispatch/                # 业务逻辑层
│   └── src/main/kotlin/com/tencent/devops/dispatch/
│       ├── configuration/           # 配置类
│       ├── controller/              # API 实现
│       ├── cron/                    # 定时任务
│       ├── dao/                     # 数据访问层
│       ├── exception/               # 异常定义
│       ├── listener/                # 消息监听
│       ├── service/                 # 业务服务
│       │   ├── jobquota/            # 作业配额服务
│       │   └── tpaqueue/            # 第三方构建机队列
│       └── utils/                   # 工具类
│
├── biz-dispatch-docker/         # Docker 调度业务
├── biz-dispatch-kubernetes/     # Kubernetes 调度业务
├── common-dispatch-kubernetes/  # Kubernetes 通用组件
├── model-dispatch/              # 数据模型层
└── boot-dispatch/               # Spring Boot 启动模块
```

### 1.2 调度类型

| 类型 | 说明 | 实现模块 |
|------|------|----------|
| **第三方构建机** | 用户自有构建机 | `biz-dispatch` |
| **Docker 构建机** | 公共 Docker 容器 | `biz-dispatch-docker` |
| **Kubernetes** | K8s Pod 构建 | `biz-dispatch-kubernetes` |

## 二、核心概念

### 2.1 构建任务状态

```kotlin
enum class PipelineTaskStatus(val status: Int) {
    QUEUE(0),           // 排队中
    RUNNING(1),         // 运行中
    DONE(2),            // 已完成
    FAILURE(3),         // 失败
    CANCEL(4),          // 取消
}
```

### 2.2 构建机类型

```kotlin
enum class JobQuotaVmType(val type: String) {
    DOCKER("DOCKER"),                   // Docker 构建机
    THIRD_PARTY_AGENT("THIRD_PARTY"),   // 第三方构建机
    KUBERNETES("KUBERNETES"),           // K8s 构建机
    AGENTLESS("AGENTLESS"),             // 无编译环境
}
```

### 2.3 第三方构建机任务类型

```kotlin
enum class BuildJobType(val type: String) {
    ALL("ALL"),           // 所有类型
    DOCKER("DOCKER"),     // Docker 构建
    BINARY("BINARY"),     // 二进制构建
}
```

## 三、核心数据库表

### 3.1 调度记录表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_DISPATCH_PIPELINE_BUILD` | 流水线构建调度记录 | `PROJECT_ID`, `PIPELINE_ID`, `BUILD_ID`, `VM_SEQ_ID`, `STATUS` |
| `T_DISPATCH_THIRDPARTY_AGENT_BUILD` | 第三方构建机任务 | `PROJECT_ID`, `AGENT_ID`, `BUILD_ID`, `STATUS`, `WORKSPACE` |
| `T_DISPATCH_PIPELINE_DOCKER_BUILD` | Docker 构建任务 | `BUILD_ID`, `VM_SEQ_ID`, `STATUS`, `DOCKER_IP`, `CONTAINER_ID` |
| `T_DISPATCH_PIPELINE_DOCKER_TASK` | Docker 任务详情 | `PROJECT_ID`, `AGENT_ID`, `IMAGE_NAME`, `STATUS` |

### 3.2 配额管理表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_DISPATCH_QUOTA_PROJECT` | 项目配额 | `PROJECT_ID`, `VM_TYPE`, `RUNNING_JOBS_MAX`, `RUNNING_TIME_PROJECT_MAX` |
| `T_DISPATCH_QUOTA_SYSTEM` | 系统配额 | `VM_TYPE`, `RUNNING_JOBS_MAX_SYSTEM`, `RUNNING_TIME_JOB_MAX` |
| `T_DISPATCH_RUNNING_JOBS` | 运行中的任务 | `PROJECT_ID`, `VM_TYPE`, `BUILD_ID`, `AGENT_START_TIME` |

### 3.3 队列管理表

| 表名 | 说明 |
|------|------|
| `T_DISPATCH_THIRDPARTY_AGENT_QUEUE` | 第三方构建机任务队列 |
| `T_DISPATCH_PIPELINE_DOCKER_DEBUG` | Docker 调试会话 |

### 3.4 字段说明

> ⚠️ **重要**: `PROJECT_ID` 是 `T_PROJECT.english_name`

## 四、分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Process 模块                                     │
│                    (流水线引擎发起调度请求)                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼ (MQ 消息)
┌─────────────────────────────────────────────────────────────────────────┐
│                         Dispatch 模块                                    │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      消息监听层 (listener/)                       │   │
│  │  ThirdPartyBuildListener    - 第三方构建机任务监听                │   │
│  │  TPAQueueListener           - 第三方构建机队列监听                │   │
│  │  TPAMonitorListener         - 第三方构建机监控监听                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      核心服务层 (service/)                        │   │
│  │  ThirdPartyDispatchService (45KB) - 第三方构建机调度核心          │   │
│  │  ThirdPartyAgentService (38KB)    - 第三方构建机管理              │   │
│  │  TPAQueueService                  - 任务队列服务                  │   │
│  │  TPAEnvQueueService               - 环境队列服务                  │   │
│  │  JobQuotaManagerService           - 配额管理服务                  │   │
│  │  AuthBuildService                 - 构建认证服务                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      DAO 层 (dao/)                                │   │
│  │  ThirdPartyAgentBuildDao (17KB)   - 第三方构建任务访问            │   │
│  │  RunningJobsDao                   - 运行任务访问                  │   │
│  │  JobQuotaProjectDao               - 项目配额访问                  │   │
│  │  TPAQueueDao                      - 队列访问                      │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         构建机 (Agent)                                   │
│                    (第三方构建机 / Docker / K8s)                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 五、核心类速查

### 5.1 API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `BuildAgentBuildResource` | `/build/agent` | 构建机与调度服务交互 |
| `ServiceAgentResource` | `/service/agent` | 服务间构建机操作 |
| `ServiceDispatchJobResource` | `/service/dispatch/job` | 服务间任务调度 |
| `OpJobQuotaProjectResource` | `/op/quota/project` | 运维项目配额管理 |
| `OpJobQuotaSystemResource` | `/op/quota/system` | 运维系统配额管理 |
| `ExternalAuthBuildResource` | `/external/auth` | 外部构建认证 |

### 5.2 Service 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `ThirdPartyDispatchService` | 45KB | 第三方构建机调度核心（最大） |
| `ThirdPartyAgentService` | 38KB | 第三方构建机管理 |
| `TPAEnvQueueService` | 23KB | 环境队列服务 |
| `JobQuotaBusinessService` | 14KB | 配额业务服务 |
| `TPAQueueService` | 13KB | 任务队列服务 |
| `TPASingleQueueService` | 13KB | 单队列服务 |
| `ThirdPartyAgentMonitorService` | 13KB | 构建机监控 |
| `ThirdPartyAgentDockerService` | 13KB | Docker 构建服务 |

### 5.3 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `ThirdPartyAgentBuildDao` | 17KB | 第三方构建任务访问 |
| `RunningJobsDao` | 8KB | 运行任务访问 |
| `TPAQueueDao` | 6KB | 队列访问 |
| `DispatchPipelineBuildDao` | 5KB | 流水线构建访问 |
| `JobQuotaSystemDao` | 5KB | 系统配额访问 |
| `JobQuotaProjectDao` | 5KB | 项目配额访问 |

## 六、核心流程

### 6.1 第三方构建机调度流程

```
Process 模块发送调度消息
    │
    ▼
ThirdPartyBuildListener.onBuildStart()
    │
    ▼
ThirdPartyDispatchService.dispatch()
    │
    ├─► 检查配额
    │   └─► JobQuotaManagerService.checkJobQuota()
    │
    ├─► 选择构建机
    │   ├─► 根据环境 ID 筛选
    │   ├─► 根据 Agent 状态筛选
    │   └─► 负载均衡选择
    │
    ├─► 创建调度记录
    │   └─► ThirdPartyAgentBuildDao.create()
    │
    └─► 通知 Agent
        └─► Redis 发布任务消息
```

### 6.2 Agent 领取任务流程

```
Agent 轮询请求任务
    │
    ▼
BuildAgentBuildResource.claimTask()
    │
    ▼
ThirdPartyAgentService.claimTask()
    │
    ├─► 验证 Agent 身份
    │   └─► 检查 Agent ID 和 Secret Key
    │
    ├─► 查询待执行任务
    │   └─► ThirdPartyAgentBuildDao.getQueueTask()
    │
    ├─► 更新任务状态
    │   └─► status = RUNNING
    │
    └─► 返回任务信息
        └─► ThirdPartyBuildInfo
```

### 6.3 构建完成流程

```
Agent 报告构建完成
    │
    ▼
BuildAgentBuildResource.finishTask()
    │
    ▼
ThirdPartyAgentService.finishTask()
    │
    ├─► 更新任务状态
    │   └─► status = DONE / FAILURE
    │
    ├─► 释放配额
    │   └─► JobQuotaManagerService.releaseQuota()
    │
    └─► 通知 Process 模块
        └─► 发送构建完成事件
```

## 七、配额管理

### 7.1 配额维度

| 维度 | 说明 |
|------|------|
| **系统级** | 全局最大并发数、单任务最大时长 |
| **项目级** | 项目最大并发数、项目最大运行时长 |
| **构建机类型** | 按 Docker/第三方/K8s 分别限制 |

### 7.2 配额检查

```kotlin
// 检查配额是否足够
fun checkJobQuota(
    projectId: String,
    vmType: JobQuotaVmType,
    buildId: String
): Boolean {
    // 1. 检查系统级配额
    val systemQuota = jobQuotaSystemDao.get(vmType)
    val systemRunning = runningJobsDao.countByVmType(vmType)
    if (systemRunning >= systemQuota.runningJobsMax) {
        return false
    }
    
    // 2. 检查项目级配额
    val projectQuota = jobQuotaProjectDao.get(projectId, vmType)
    val projectRunning = runningJobsDao.countByProject(projectId, vmType)
    if (projectRunning >= projectQuota.runningJobsMax) {
        return false
    }
    
    return true
}
```

## 八、与其他模块的关系

### 8.1 依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    Dispatch 模块依赖关系                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────┐                                                  │
│  │  process  │ ──────► 发送调度消息                              │
│  └───────────┘                                                  │
│        │                                                         │
│        ▼                                                         │
│  ┌───────────┐                                                  │
│  │ dispatch  │                                                  │
│  └─────┬─────┘                                                  │
│        │                                                         │
│        ├──────► project (项目信息)                               │
│        ├──────► environment (构建机信息)                         │
│        └──────► auth (权限校验)                                  │
│                                                                  │
│  被依赖：                                                        │
│  - agent（构建机领取任务）                                        │
└─────────────────────────────────────────────────────────────────┘
```

### 8.2 服务间调用示例

```kotlin
// Process 模块查询调度状态
client.get(ServiceDispatchJobResource::class).getDispatchJob(
    projectId = projectId,  // english_name
    pipelineId = pipelineId,
    buildId = buildId,
    vmSeqId = vmSeqId
)

// Environment 模块获取 Agent 信息
client.get(ServiceThirdPartyAgentResource::class).getAgentById(
    projectId = projectId,
    agentId = agentId
)
```

## 九、开发规范

### 9.1 新增调度类型

1. 在 `JobQuotaVmType` 添加新类型
2. 创建对应的调度服务实现
3. 在配额表中添加新类型的配置
4. 实现任务分发和状态管理逻辑

### 9.2 调度记录查询示例

```kotlin
// 查询构建任务
val task = thirdPartyAgentBuildDao.get(
    dslContext = dslContext,
    buildId = buildId,
    vmSeqId = vmSeqId
)

// 查询 Agent 的运行任务
val runningTasks = thirdPartyAgentBuildDao.listByAgentId(
    dslContext = dslContext,
    agentId = agentId,
    status = PipelineTaskStatus.RUNNING.status
)
```

## 十、常见问题

**Q: 如何选择构建机？**
A: 根据环境 ID、Agent 状态、负载情况综合选择，优先选择空闲的 Agent。

**Q: 配额不足时任务会怎样？**
A: 任务会进入队列等待，直到有配额释放。

**Q: Agent 离线后任务会怎样？**
A: 任务会被标记为失败，或者重新调度到其他 Agent。

**Q: 如何调整项目配额？**
A: 通过运维接口 `OpJobQuotaProjectResource` 调整。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
