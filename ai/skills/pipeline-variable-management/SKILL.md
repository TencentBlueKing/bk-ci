---
name: pipeline-variable-management
description: 流水线变量管理指南，涵盖变量生命周期（创建、初始化、更新、存储、传递、查询）和变量字段扩展（字段定义、类型扩展、作用域、继承）。当用户开发变量功能、处理变量传递、扩展变量字段、调试变量问题、配置 pipeline variables 或处理 CI/CD 变量参数时使用。
---

# 流水线变量管理

## 变量数据模型

BK-CI 流水线变量有两种模型，通过 `VariableTransfer` 双向转换：

| 模型 | 文件 | 用途 |
|------|------|------|
| `BuildFormProperty` | `common-pipeline/.../BuildFormProperty.kt` | 后端内部模型，数据库存储，Swagger 注解 |
| `Variable` | `common-pipeline-yaml/.../yaml/v3/models/Variable.kt` | YAML 定义，对外 API，Jackson 注解 |
| `VariableTransfer` | `common-pipeline-yaml/.../yaml/transfer/VariableTransfer.kt` | 双向转换：`makeVariableFromModel()` / `makeVariableFromYaml()` |

## 核心服务与存储

| 类/表 | 位置 | 职责 |
|-------|------|------|
| `PipelineVariableService` | `process/biz-base/.../PipelineVariableService.kt` | 变量业务逻辑 |
| `BuildVariableService` | `process/biz-base/.../BuildVariableService.kt` | 构建变量管理 |
| `VariableAcrossInfoUtil` | `process/biz-base/.../VariableAcrossInfoUtil.kt` | 跨 Job 变量传递 |
| `T_PIPELINE_BUILD_VAR` | 数据库 | 构建级别的运行时变量实例 |
| `T_PIPELINE_SETTING` | 数据库 | 流水线配置（含变量定义） |

## 常用操作代码示例

### 读取构建变量

```kotlin
// 获取单个变量
val value = buildVariableService.getVariable(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    varName = "MY_VAR"
)

// 获取构建的所有变量
val allVars = buildVariableService.getAllVariable(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId
)
```

### 批量更新变量（执行期间）

```kotlin
// 插件完成时更新变量（需加分布式锁）
buildVariableService.batchUpdateVariable(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    variables = mapOf("output_key" to "output_value")
)
// 底层调用 PipelineBuildVarDao.batchUpdate()
// 锁：PipelineBuildVarLock (Redis 分布式锁)
```

### 跨 Job 传递变量

```kotlin
// Job-A 输出变量：通过 steps.<stepId>.outputs.<key>
// Job-B 读取：${{ jobs.<jobId>.steps.<stepId>.outputs.<key> }}

// 跨 Job 工具类
VariableAcrossInfoUtil.setVariableAcross(
    buildId = buildId,
    stageId = stageId,
    jobId = jobId,
    variables = acrossVariables
)
```

## 变量生命周期流程

1. **创建初始化** — `StartBuildContext.init()` 合并系统预置变量 (`BK_CI_BUILD_ID` 等) + 用户启动参数 + 流水线定义变量 → 批量写入 `T_PIPELINE_BUILD_VAR`
2. **动态更新** — Worker 端插件输出 → `output.json` → `completeTask` API → Engine 端 `BuildVariableService.batchUpdateVariable()`（Redis 锁保护）
3. **跨容器传递** — `VariableAcrossInfoUtil` 处理 Job 间、Stage 间变量传递；父子流水线通过 `SubPipelineStartUpService.callPipelineStartup()` 传递
4. **表达式解析** — 运行时通过表达式引擎解析 `${{ variables.xxx }}`，详见 `utility-components` Skill

详细流程：[reference/1-lifecycle.md](reference/1-lifecycle.md)

## 变量字段扩展流程

扩展变量字段时，按以下顺序修改（每步完成后验证编译）：

1. **定义 YAML 模型字段** — 在 `Variable.kt` 添加字段（可空类型 + `@JsonProperty` + `@Schema`）
   - 验证：`./gradlew :common:common-pipeline-yaml:compileKotlin`
2. **定义内部模型字段** — 在 `BuildFormProperty.kt` 添加对应字段（camelCase，放在末尾）
   - 验证：`./gradlew :common:common-pipeline:compileKotlin`
3. **更新转换器** — 在 `VariableTransfer.kt` 的 `makeVariableFromModel()` 和 `makeVariableFromYaml()` 中处理新字段
   - **同时更新** `YamlObjects.kt` 的 `getVariable()` 方法（独立解析路径，必须同步修改）
   - 验证：`./gradlew :common:common-pipeline-yaml:compileKotlin`
4. **前端同步** — 更新 Vue 组件中的变量属性定义
5. **运行测试** — `./gradlew :process:test --tests '*Variable*'`

详细指南与实际案例：[reference/2-extension.md](reference/2-extension.md)

## 故障排查清单

| 问题 | 排查步骤 |
|------|----------|
| 变量值为空/不正确 | 1. 查 `T_PIPELINE_BUILD_VAR` 表确认写入值 2. 检查变量名拼写（区分大小写）3. 确认变量作用域覆盖当前 Job |
| 跨 Job/Stage 变量丢失 | 1. 确认使用 `${{ jobs.<jobId>.steps.<stepId>.outputs.<key> }}` 语法 2. 检查 `VariableAcrossInfoUtil` 调用是否正确设置了 stageId/jobId |
| 新增字段前端不显示 | 1. 确认 `Variable.kt` 和 `BuildFormProperty.kt` 都已添加字段 2. 检查 `VariableTransfer` 是否处理了转换 3. 确认 Vue 组件已同步 |
| 变量更新并发冲突 | 检查 `PipelineBuildVarLock` Redis 锁是否正常获取，查看 Engine 日志中的锁相关异常 |

## 相关 Skill

- **`pipeline-model-architecture`** — 变量在 Model 中的位置
- **`process-module-architecture`** — 变量处理的代码架构
- **`utility-components`** (reference/2-expression-parser.md) — 变量表达式解析

## 详细文档

| 文档 | 内容 |
|------|------|
| [1-lifecycle.md](reference/1-lifecycle.md) | 变量生命周期完整流程（1414 行） |
| [2-extension.md](reference/2-extension.md) | 变量字段扩展指南（534 行） |
