---
name: worker-module-architecture
description: BK-CI Worker 构建执行器模块开发指南。当需要开发 Worker 新功能、实现自定义任务类型、调试插件执行、处理任务分发逻辑、优化构建性能、排查构建机问题时使用。涵盖插件执行引擎、Runner 生命周期、日志上报、制品归档、心跳机制。
---

# Worker 构建执行器模块架构指南

## 模块概述

Worker 是 BK-CI 构建执行器，运行在构建机（Agent）上，负责接收并执行流水线任务。

**源码路径**: `src/backend/ci/core/worker/`

### 运行模式

| 类型 | 说明 | 场景 |
|------|------|------|
| `DOCKER` | Docker 容器构建 | 公共构建资源池 |
| `AGENT` | 第三方构建机 | 用户自有构建机 |
| `WORKER` | 无编译环境 | 轻量级任务执行 |

### 目录结构

```
src/backend/ci/core/worker/
├── worker-agent/          # Agent 启动入口（Application.kt）
├── worker-common/         # 公共组件（Runner、任务框架、日志、心跳）
│   └── com/tencent/devops/worker/common/
│       ├── Runner.kt      # 核心运行器
│       ├── task/           # 任务执行框架（ITask、TaskFactory）
│       ├── logger/         # LoggerService
│       ├── heartbeat/      # Heartbeat
│       └── service/        # EngineService
└── worker-api-sdk/        # API SDK（制品、插件、日志、流水线等）
```

---

## 核心流程

### Worker 启动与任务循环

```
Runner.run(workspaceInterface)
  ├── 1. EngineService.setStarted()   → 上报启动，获取 BuildVariables
  │   ✅ 验证: buildVariables.projectId 和 pipelineId 非空
  ├── 2. LoggerService.start()        → 启动日志缓冲队列
  ├── 3. Heartbeat.start()            → 启动心跳（每2秒）+ Job 超时监控
  ├── 4. loopPickup()                 → 循环领取任务
  │   ├── claimTask()                 → 获取 BuildTask
  │   │   ✅ 验证: status == DO 才执行，WAIT 则等待，END 则退出
  │   ├── TaskFactory.create(type)    → 按 classType 创建 ITask 实例
  │   │   ✅ 验证: 未注册类型返回 EmptyTask，检查日志确认是否缺少插件
  │   ├── taskDaemon.run()            → 执行任务
  │   └── completeTask()              → 上报 BuildTaskResult
  │       ✅ 验证: errorType 和 errorCode 在失败时必须设置
  └── 5. finishWorker()               → endBuild() 结束构建
```

### 心跳机制要点

- 每 2 秒发送一次，连续 **12 次失败后 Worker 自动退出**（`exitProcess(-1)`）
- 心跳响应携带 `cancelTaskIds`，用于中止正在运行的任务
- Job 超时由 `Heartbeat` 内的定时器监控，超时后调用 `EngineService.timeout()` 并 `exitProcess(99)`

---

## 新增任务类型

### 步骤

1. 创建任务类，放在 `com.tencent.devops.plugin.worker.task` 包下（自动扫描注册）：

```kotlin
@TaskClassType(classTypes = ["myTaskType"], priority = 1)
class MyTask : ITask() {
    override fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        LoggerService.addNormalLine("执行任务...")
        // 业务逻辑
        addEnv(mapOf("OUTPUT_VAR" to "value"))  // 输出变量
    }
}
```

2. 验证注册：启动 Worker 后在 `TaskFactory.init()` 日志中确认 `myTaskType` 已注册

> **注意**: `addEnv()` 会校验只读变量和变量名合法性，非法变量会被静默忽略

### 任务类型参考

见 [reference/task-types.md](reference/task-types.md) 获取完整的内置任务和插件任务列表。

---

## 插件执行流程（MarketAtomTask）

```
1. atomApi.getAtomEnv(projectCode, atomCode, atomVersion)
   ✅ 验证: 返回的执行命令和包地址非空
2. 准备执行环境
   ├── 创建临时工作目录
   ├── 下载/缓存插件包
   └── SHA 完整性校验
       ✅ 验证: SHA 不匹配时抛出异常，检查插件版本是否正确
3. 准备输入参数
   ├── 解析 task.params → input 参数
   ├── 替换变量和 ${{credentials.xxx}} 凭据
   └── 写入 .sdk.json（SDK 环境信息）
4. 执行插件
   ├── Linux: ShellUtil.execute()
   └── Windows: BatScriptUtil.execute()
5. 处理 output.json
   ├── status: success/failure
   ├── string 类型 → 输出变量
   ├── artifact 类型 → 归档制品
   └── report 类型 → 归档报告
   ✅ 验证: output.json 必须存在且 JSON 格式有效，否则任务标记失败
```

### 插件包缓存

- 第三方构建机：`{user.dir}/store/cache/plugins/{atomCode}-{version}-{fileName}`
- 公共构建机：`{workspace}/../cache/store/cache/plugins/`
- 测试版本插件不缓存

---

## 日志输出

```kotlin
LoggerService.addNormalLine("正常信息")
LoggerService.addWarnLine("警告信息")      // 黄色
LoggerService.addErrorLine("错误信息")     // 红色
LoggerService.addDebugLine("调试信息")     // 灰色

// 折叠日志块
LoggerService.addFoldStartLine("[安装依赖]")
// ... 详细日志
LoggerService.addFoldEndLine("-----")
```

日志队列容量 2000 条 `LogMessage`，满时阻塞。敏感信息通过 `SensitiveValueService.addSensitiveValue()` 注册后自动替换为 `******`。

---

## 错误处理

```kotlin
throw TaskExecuteException(
    errorMsg = "任务执行失败",
    errorType = ErrorType.USER,        // USER/SYSTEM/THIRD_PARTY/PLUGIN
    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
)
```

**ErrorType 选择指南**：
- `USER`: 用户配置或操作错误（脚本语法、参数缺失）
- `SYSTEM`: BK-CI 系统内部错误
- `THIRD_PARTY`: 外部服务故障（Git 仓库不可达、制品库超时）
- `PLUGIN`: 插件自身 Bug

---

## 与其他模块的关系

| 依赖模块 | 用途 |
|----------|------|
| Process | 任务领取、状态上报、心跳 |
| Log | 日志上报 |
| Store | 插件信息获取、插件下载 |
| Artifactory | 制品上传下载 |
| Ticket | 凭据获取（`${{credentials.xxx}}`） |
| Repository | 代码库信息 |

---

## 调试与排查

| 场景 | 方法 |
|------|------|
| 开启调试日志 | 设置 `DEVOPS_AGENT_LOG_DEBUG=true` |
| 插件执行失败 | 检查 `output.json` → Worker 日志 → SHA 校验 → 运行环境版本 |
| 心跳失败 | 检查网络连通性，连续12次失败 Worker 自动退出 |
| 国际化 | `support-files/i18n/worker/message_{locale}.properties`，常量在 `WorkerMessageCode.kt` |

---

## 参考文件

- [API 客户端与认证头](reference/api-clients.md) — EngineService 方法、API 客户端列表、认证头
- [数据模型](reference/data-models.md) — BuildTask、BuildVariables、BuildTaskResult、SdkEnv、output.json 格式
- [任务类型与工具类](reference/task-types.md) — 内置/插件任务清单、ShellUtil、CredentialUtils、WorkspaceUtils
