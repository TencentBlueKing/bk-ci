---
name: pipeline-model-architecture
description: BK-CI 流水线核心模型（Model）架构详解，涵盖 Pipeline/Stage/Container/Task 四层结构、模型序列化、版本管理、模型校验。当用户理解流水线数据结构、开发流水线功能、处理模型转换或进行模型扩展时使用。
---

# BK-CI 流水线核心模型 (Model) 架构详解

## Skill 概述

**Skill 名称**: Pipeline Model Architecture  
**适用场景**: 理解和操作 BK-CI 流水线的核心数据结构  
**重要性**: ⭐⭐⭐⭐⭐ (最高优先级)  
**文档版本**: 2.0  
**最后更新**: 2024-12

Model 是整个 BK-CI 流水线系统的**核心数据模型**，定义了流水线在内部系统中的完整数据结构。所有流水线相关的业务逻辑（创建、编辑、执行、调度、监控）都围绕这个模型展开。

### 为什么 Model 如此重要？

1. **数据载体**: Model 是流水线配置的唯一数据载体，前端编排、后端存储、构建执行都依赖它
2. **版本控制**: 每次流水线修改都会生成新版本的 Model
3. **构建快照**: 每次构建都会保存 Model 快照，支持历史回溯和重试
4. **跨系统通信**: API 接口、微服务间通信都以 Model 为核心数据结构
5. **扩展基础**: 新增流水线功能（如新插件类型）必须理解 Model 架构

---

## 一、Model 架构概览

### 1.1 核心层次结构

Model 采用**四层嵌套**的树状结构，完整描述了一个流水线的组织方式：

```
Model (流水线)
  └── Stage[] (阶段集合)
        └── Container[] (容器/Job集合)
              └── Element[] (插件/任务集合)
```

**数据文件位置**:
```kotlin
// 核心模型定义
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/Model.kt
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/container/Stage.kt
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/container/Container.kt
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/Element.kt
```

### 1.2 设计原则

- **多态性**: Container 和 Element 都采用接口 + 实现类的多态设计
- **Jackson 序列化**: 使用 `@JsonTypeInfo` 和 `@JsonSubTypes` 注解实现多态 JSON 序列化
- **运行时中间参数**: 很多字段（如 `status`、`executeCount`）标记为 `readOnly = true`，仅在构建运行时使用
- **版本兼容性**: 包含 `@Deprecated` 字段和 `transformCompatibility()` 方法处理历史数据

---

## 二、Model 顶层结构详解

### 2.1 Model 类定义

```kotlin
data class Model(
    var name: String,                           // 流水线名称
    var desc: String?,                          // 流水线描述
    val stages: List<Stage>,                    // 阶段集合（核心）
    var labels: List<String> = emptyList(),     // 标签（已废弃）
    
    // 模板相关
    val instanceFromTemplate: Boolean? = null,  // 是否从模板实例化
    var srcTemplateId: String? = null,          // 源模板ID
    var templateId: String? = null,             // 当前模板ID
    var template: TemplateInstanceDescriptor?,  // 模板实例描述符
    var overrideTemplateField: TemplateInstanceField?, // 覆盖模板字段
    
    // 元数据
    var pipelineCreator: String? = null,        // 创建人
    var latestVersion: Int = 0,                 // 最新版本号
    var tips: String? = null,                   // 提示信息
    
    // 事件和视图
    var events: Map<String, PipelineCallbackEvent>?, // 流水线回调事件
    var staticViews: List<String> = emptyList(), // 静态流水线组
    
    // 运行时数据
    var timeCost: BuildRecordTimeCost? = null,  // 各项耗时统计
    val resources: Resources? = null            // 模板资源
)
```

### 2.2 核心方法

#### 2.2.1 获取触发容器

```kotlin
@JsonIgnore
fun getTriggerContainer() = stages[0].containers[0] as TriggerContainer
```

**说明**: 
- 触发容器永远在第一个 Stage 的第一个 Container 位置
- 这是流水线的入口点，包含触发器和流水线参数

#### 2.2.2 统计任务数量

```kotlin
fun taskCount(skipTaskClassType: Set<String> = emptySet()): Int {
    var count = 0
    stages.forEach { s ->
        s.containers.forEach { c ->
            c.elements.forEach { e ->
                if (!skipTaskClassType.contains(e.getClassType())) {
                    count++
                }
            }
        }
    }
    return count
}
```

#### 2.2.3 删除指定类型原子

```kotlin
fun removeElements(elementClassTypes: Set<String>): Model {
    // 遍历所有 Stage、Container、Element
    // 过滤掉指定 classType 的 Element
    // 返回新的 Model 实例
}
```

**使用场景**: 模板清理、插件卸载时移除特定类型的任务

#### 2.2.4 默认模型工厂方法

```kotlin
companion object {
    fun defaultModel(
        pipelineName: String = "",
        userId: String? = null
    ): Model {
        return Model(
            name = pipelineName,
            desc = "",
            stages = listOf(
                Stage(
                    id = "stage-1",
                    containers = listOf(
                        TriggerContainer(
                            id = "0",
                            name = "trigger",
                            elements = listOf(
                                ManualTriggerElement(
                                    id = "T-1-1-1",
                                    name = I18nUtil.getCodeLanMessage(
                                        CommonMessageCode.BK_MANUAL_TRIGGER
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            pipelineCreator = userId
        )
    }
}
```

**说明**: 创建一个最小化的流水线模型，只包含一个手动触发器

---

## 三、Stage (阶段) 详解

### 3.1 Stage 数据结构

```kotlin
data class Stage(
    val containers: List<Container> = listOf(),  // 容器集合（核心）
    var id: String?,                             // 系统生成的ID（不可编辑）
    var name: String? = "",                      // 阶段名称
    var stageIdForUser: String? = null,          // 用户可编辑的ID
    
    // 流程控制
    var stageControlOption: StageControlOption?, // 流程控制选项
    var checkIn: StagePauseCheck? = null,        // Stage准入配置（人工审核）
    var checkOut: StagePauseCheck? = null,       // Stage准出配置（人工审核）
    val finally: Boolean = false,                // 是否为FinallyStage
    val fastKill: Boolean? = false,              // 失败快速终止
    
    // 运行时数据
    var status: String? = null,                  // 阶段状态
    var executeCount: Int? = null,               // 运行次数
    var canRetry: Boolean? = null,               // 是否可重试
    var timeCost: BuildRecordTimeCost? = null,   // 各项耗时
    
    // 其他
    val customBuildEnv: Map<String, String>?,    // 自定义环境变量
    var tag: List<String>? = null,               // 阶段标签（显示用）
    var template: TemplateDescriptor? = null     // 模板信息
)
```

### 3.2 Stage 核心概念

#### 3.2.1 FinallyStage

```kotlin
val finally: Boolean = false
```

**特性**:
- 每个 Model 只能包含一个 FinallyStage
- 必须处于最后位置
- **无论流水线成功或失败都会执行**（类似 try-finally）
- FinallyStage 不可重试 (`canRetry = false`)

**使用场景**:
- 资源清理
- 通知发送
- 日志归档

#### 3.2.2 Stage 准入准出 (CheckIn/CheckOut)

```kotlin
var checkIn: StagePauseCheck? = null   // 阶段开始前人工审核
var checkOut: StagePauseCheck? = null  // 阶段结束后人工审核
```

**StagePauseCheck 包含**:
- `reviewGroups`: 审核人组
- `timeout`: 审核超时时间
- `reviewParams`: 审核时填写的参数
- `status`: 审核状态

**流程**:
```
Stage 开始 → checkIn 审核 → 执行 Containers → checkOut 审核 → Stage 结束
```

### 3.3 Stage 核心方法

#### 3.3.1 重置构建选项

```kotlin
fun resetBuildOption(init: Boolean? = false) {
    if (init == true) {
        status = null
        startEpoch = null
        elapsed = null
    }
    checkIn?.fixReviewGroups(init == true)
    checkOut?.fixReviewGroups(init == true)
    
    // 如果配置了手动触发但没有checkIn，自动创建
    if (stageControlOption?.manualTrigger == true && checkIn == null) {
        checkIn = StagePauseCheck.convertControlOption(stageControlOption!!)
    }
    
    if (finally) canRetry = false  // FinallyStage禁止重试
}
```

#### 3.3.2 获取容器

```kotlin
fun getContainer(vmSeqId: String): Container? {
    containers.forEach { container ->
        return container.getContainerById(vmSeqId) ?: return@forEach
    }
    return null
}
```

#### 3.3.3 Stage 是否启用

```kotlin
fun stageEnabled(): Boolean {
    return stageControlOption?.enable ?: true
}
```

---

## 四、Container (容器/Job) 详解

### 4.1 Container 接口定义

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TriggerContainer::class, name = TriggerContainer.classType),
    JsonSubTypes.Type(value = NormalContainer::class, name = NormalContainer.classType),
    JsonSubTypes.Type(value = VMBuildContainer::class, name = VMBuildContainer.classType),
    JsonSubTypes.Type(value = JobTemplateContainer::class, name = JobTemplateContainer.classType)
)
interface Container {
    var id: String?                    // 序列ID
    var name: String                   // 容器名称
    var elements: List<Element>        // 任务集合（核心）
    
    // 运行时状态
    var status: String?
    var startVMStatus: String?         // 构建环境启动状态
    var executeCount: Int?             // 运行次数
    var canRetry: Boolean?             // 是否可重试
    
    // 唯一标识
    var containerId: String?           // 容器唯一ID（同id）
    var containerHashId: String?       // 容器全局唯一HashID
    var jobId: String?                 // 用户自定义ID
    
    // 耗时统计
    var timeCost: BuildRecordTimeCost?
    var startVMTaskSeq: Int?           // 开机任务序号
    
    // 标志位
    var containPostTaskFlag: Boolean?  // 是否包含post任务
    val matrixGroupFlag: Boolean?      // 是否为构建矩阵
    
    // 抽象方法
    fun getClassType(): String
    fun getContainerById(vmSeqId: String): Container?
    fun containerEnabled(): Boolean
    fun setContainerEnable(enable: Boolean)
    fun resetBuildOption(executeCount: Int)
    fun transformCompatibility()
    fun genTaskParams(): MutableMap<String, Any>
    fun copyElements(elements: List<Element>): Container
    
    // 矩阵相关
    fun retryFreshMatrixOption()
    fun fetchGroupContainers(): List<Container>?
    fun fetchMatrixContext(): Map<String, String>?
}
```

### 4.2 Container 三大实现类

#### 4.2.1 TriggerContainer (触发容器)

```kotlin
data class TriggerContainer(
    override var id: String? = null,
    override var name: String = "",
    override var elements: List<Element> = listOf(),  // 触发器集合
    
    var params: List<BuildFormProperty> = listOf(),   // 流水线参数
    var templateParams: List<BuildFormProperty>?,     // 模板参数
    var buildNo: BuildNo? = null,                     // 构建版本号规则
    
    // ... 其他 Container 接口字段
) : Container {
    companion object {
        const val classType = "trigger"
    }
}
```

**特点**:
- 永远在 `stages[0].containers[0]` 位置
- 包含流水线的**全局参数定义**
- 包含**触发器** (ManualTriggerElement, TimerTriggerElement, CodeWebHookTriggerElement 等)
- 只有一个，不可删除

**流水线参数 (BuildFormProperty)**:
```kotlin
data class BuildFormProperty(
    var id: String,                      // 参数ID
    var name: String?,                   // 参数名称
    var required: Boolean,               // 是否必填
    var type: BuildFormPropertyType,     // 参数类型 (STRING, BOOLEAN, ENUM, SVN_TAG等)
    var defaultValue: Any,               // 默认值
    var value: Any? = null,              // 上次构建取值
    var options: List<BuildFormValue>?,  // 下拉选项
    var desc: String?,                   // 描述
    
    // 特殊字段
    var asInstanceInput: Boolean? = null // 控制实例化页面"实例入参"按钮
)
```

#### 4.2.2 VMBuildContainer (虚拟机构建容器)

```kotlin
data class VMBuildContainer(
    override var id: String? = null,
    override var name: String = "构建环境",
    override var elements: List<Element> = listOf(),
    
    // 构建机配置
    val baseOS: VMBaseOS,                      // 基础操作系统 (LINUX, WINDOWS, MACOS)
    val vmNames: Set<String> = setOf(),        // 预指定VM名称列表
    val dispatchType: DispatchType? = null,    // 构建机调度类型
    
    // 环境变量
    val buildEnv: Map<String, String>?,        // 容器启动时环境变量
    val customEnv: List<NameAndValue>?,        // Agent启动时自定义环境变量
    
    // 第三方构建机
    val thirdPartyAgentId: String? = null,
    val thirdPartyAgentEnvId: String? = null,
    val thirdPartyWorkspace: String? = null,
    
    // 流程控制
    var jobControlOption: JobControlOption?,   // Job控制选项
    var mutexGroup: MutexGroup?,               // 互斥组
    
    // 构建矩阵
    var matrixControlOption: MatrixControlOption?,  // 矩阵配置
    var groupContainers: MutableList<VMBuildContainer>?,  // 分裂后的子容器
    var matrixGroupId: String?,                     // 所属矩阵组ID
    var matrixContext: Map<String, String>?,        // 矩阵上下文
    
    var showBuildResource: Boolean? = false,
    var enableExternal: Boolean? = false,       // 是否访问外网
    var nfsSwitch: Boolean? = null              // NFS挂载开关
) : Container
```

**构建机调度类型 (DispatchType)**:
- `ThirdPartyAgentIDDispatchType`: 第三方构建机
- `ThirdPartyAgentEnvDispatchType`: 第三方环境
- `DockerDispatchType`: Docker容器
- `LocalDispatchType`: 本地调度

#### 4.2.3 NormalContainer (无编译环境容器)

```kotlin
data class NormalContainer(
    override var id: String? = null,
    override var name: String = "",
    override var elements: List<Element> = listOf(),
    
    var jobControlOption: JobControlOption?,
    var mutexGroup: MutexGroup?,
    
    // 构建矩阵支持
    var matrixControlOption: MatrixControlOption?,
    var groupContainers: MutableList<NormalContainer>?,
    var matrixGroupId: String?,
    var matrixContext: Map<String, String>?,
    
    // ... 其他 Container 接口字段
) : Container {
    companion object {
        const val classType = "normal"
    }
}
```

**特点**:
- 无需启动构建机
- 用于执行**无编译环境插件**（如人工审核、质量红线、API调用等）
- 轻量级，启动快

### 4.3 JobControlOption (Job流程控制)

```kotlin
data class JobControlOption(
    val enable: Boolean = true,                  // 是否启用Job
    val prepareTimeout: Int? = null,             // 准备环境超时时间（分钟）
    var timeout: Int? = 900,                     // 执行超时时间（分钟）
    var timeoutVar: String? = null,              // 超时时间变量（支持表达式）
    
    // 运行条件
    val runCondition: JobRunCondition = JobRunCondition.STAGE_RUNNING,
    val customVariables: List<NameAndValue>?,    // 自定义变量条件
    val customCondition: String? = null,         // 自定义条件表达式
    
    // Job依赖
    val dependOnType: DependOnType? = null,      // 依赖类型
    var dependOnId: List<String>? = null,        // 依赖的JobID列表
    val dependOnName: String? = null,
    var dependOnContainerId2JobIds: Map<String, String>?,  // containerId与jobId映射
    
    val continueWhenFailed: Boolean? = false,    // 失败继续
    
    // 并发控制（第三方构建机）
    val singleNodeConcurrency: Int? = null,      // 单节点并发限制
    val allNodeConcurrency: Int? = null          // 所有节点并发限制
)
```

**JobRunCondition 枚举**:
- `STAGE_RUNNING`: Stage运行中（默认）
- `CUSTOM_VARIABLE_MATCH`: 自定义变量匹配
- `CUSTOM_CONDITION_MATCH`: 自定义条件匹配

### 4.4 构建矩阵 (Matrix)

构建矩阵允许一个 Job 根据参数组合分裂成多个并行执行的子 Job。

```kotlin
data class MatrixControlOption(
    var strategyStr: String?,              // 策略字符串
    var includeCaseStr: String?,           // 包含用例
    var excludeCaseStr: String?,           // 排除用例
    var maxConcurrency: Int? = null,       // 最大并发数
    var totalCount: Int? = null,           // 总任务数（运行时计算）
    var finishCount: Int? = null           // 已完成数（运行时计算）
)
```

**矩阵字段**:
- `matrixGroupFlag`: 标识当前容器是否为矩阵父容器
- `groupContainers`: 分裂后的子容器集合（父容器特有）
- `matrixGroupId`: 所属矩阵组的 containerHashId（子容器特有）
- `matrixContext`: 当前子容器的参数组合（子容器特有）

**示例**:
```yaml
matrix:
  strategy:
    os: [linux, windows]
    version: [1.0, 2.0]
  # 会生成4个子Job: (linux,1.0), (linux,2.0), (windows,1.0), (windows,2.0)
```

---

## 五、Element (插件/任务) 详解

### 5.1 Element 抽象类定义

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = CodeGitElement::class, name = CodeGitElement.classType),
    JsonSubTypes.Type(value = LinuxScriptElement::class, name = LinuxScriptElement.classType),
    JsonSubTypes.Type(value = WindowsScriptElement::class, name = WindowsScriptElement.classType),
    JsonSubTypes.Type(value = ManualTriggerElement::class, name = ManualTriggerElement.classType),
    JsonSubTypes.Type(value = MarketBuildAtomElement::class, name = MarketBuildAtomElement.classType),
    // ... 更多子类型
)
abstract class Element(
    open val name: String,                       // 任务名称
    open var id: String? = null,                 // 任务ID
    
    // 运行时状态
    open var status: String? = null,             // 状态
    open var executeCount: Int = 1,              // 执行次数
    open var canRetry: Boolean? = null,          // 是否可重试
    open var retryCount: Int? = null,            // 总重试次数
    open var retryCountManual: Int? = null,      // 手动重试次数
    open var retryCountAuto: Int? = null,        // 自动重试次数
    open var canSkip: Boolean? = null,           // 是否可跳过
    
    // 插件版本
    open var version: String = "1.*",            // 插件版本
    open var originVersion: String? = null,      // 原始版本
    
    // 流程控制
    open var additionalOptions: ElementAdditionalOptions? = null,  // 附加选项
    open var stepId: String? = null,             // 用户自定义ID（用于上下文变量）
    
    // 环境变量
    open var customEnv: List<NameAndValue>? = null,  // 自定义环境变量
    
    // 错误信息
    open var errorType: String? = null,
    open var errorCode: Int? = null,
    open var errorMsg: String? = null,
    
    // 其他
    open var timeCost: BuildRecordTimeCost? = null,
    open var progressRate: Double? = null,       // 进度
    var asyncStatus: String? = null
) {
    abstract fun getClassType(): String
    open fun getAtomCode() = getClassType()
    open fun getTaskAtom(): String = ""
    open fun genTaskParams(): MutableMap<String, Any> = JsonUtil.toMutableMap(this)
    
    fun elementEnabled(): Boolean {
        return additionalOptions?.enable ?: true
    }
    
    fun transformCompatibility() {
        if (additionalOptions != null && additionalOptions!!.timeoutVar.isNullOrBlank()) {
            additionalOptions!!.timeoutVar = additionalOptions!!.timeout.toString()
        }
    }
    
    fun initStatus(rerun: Boolean = false): BuildStatus {
        return if (!elementEnabled()) {
            BuildStatus.SKIP
        } else if (rerun) {
            BuildStatus.QUEUE
        } else if (status == BuildStatus.SKIP.name) {
            BuildStatus.SKIP
        } else {
            BuildStatus.QUEUE
        }
    }
}
```

### 5.2 ElementAdditionalOptions (插件级流程控制)

```kotlin
data class ElementAdditionalOptions(
    var enable: Boolean = true,                  // 是否启用
    var continueWhenFailed: Boolean = false,     // 失败继续
    val manualSkip: Boolean? = null,             // 手动跳过按钮
    
    // 重试配置
    var retryWhenFailed: Boolean = false,        // 失败重试
    var retryCount: Int = 0,                     // 重试次数
    val manualRetry: Boolean = true,             // 允许手动重试
    
    // 超时配置
    var timeout: Long? = 100,                    // 超时时间（分钟）
    var timeoutVar: String? = null,              // 超时变量
    
    // 运行条件
    var runCondition: RunCondition? = null,
    val customVariables: List<NameAndValue>?,    // 自定义变量条件
    var customCondition: String? = "",           // 自定义条件表达式
    
    // 暂停配置
    var pauseBeforeExec: Boolean? = false,       // 执行前暂停
    var subscriptionPauseUser: String? = "",     // 订阅暂停通知用户
    
    // Post任务
    var elementPostInfo: ElementPostInfo? = null // Post信息
)
```

**RunCondition 枚举**:
```kotlin
enum class RunCondition {
    PRE_TASK_SUCCESS,              // 所有前置插件成功
    PRE_TASK_FAILED_BUT_CANCEL,    // 前置失败也运行（除非取消）
    PRE_TASK_FAILED_EVEN_CANCEL,   // 前置失败也运行（即使取消）
    PRE_TASK_FAILED_ONLY,          // 只有前置失败才运行
    CUSTOM_VARIABLE_MATCH,         // 自定义变量匹配
    CUSTOM_VARIABLE_MATCH_NOT_RUN, // 自定义变量匹配时不运行
    PARENT_TASK_CANCELED_OR_TIMEOUT, // 父任务取消或超时
    PARENT_TASK_FINISH             // 父任务结束
}
```

### 5.3 常见 Element 子类型

#### 5.3.1 触发器类

- `ManualTriggerElement`: 手动触发
- `TimerTriggerElement`: 定时触发
- `CodeGitWebHookTriggerElement`: Git WebHook触发
- `CodeGitlabWebHookTriggerElement`: GitLab WebHook触发
- `RemoteTriggerElement`: 远程触发

#### 5.3.2 代码拉取类

- `CodeGitElement`: Git拉代码
- `CodeGitlabElement`: GitLab拉代码
- `CodeSvnElement`: SVN拉代码
- `GithubElement`: GitHub拉代码

#### 5.3.3 脚本执行类

- `LinuxScriptElement`: Linux Shell脚本
- `WindowsScriptElement`: Windows Batch/PowerShell脚本

#### 5.3.4 市场插件

- `MarketBuildAtomElement`: 研发商店插件（有编译环境）
- `MarketBuildLessAtomElement`: 研发商店插件（无编译环境）

#### 5.3.5 其他

- `ManualReviewUserTaskElement`: 人工审核
- `SubPipelineCallElement`: 子流水线调用
- `QualityGateInElement`: 质量红线（准入）
- `QualityGateOutElement`: 质量红线（准出）

---

## 六、Model 持久化与序列化

### 6.1 存储方式

Model 在数据库中以 **JSON 字符串** 形式存储：

```kotlin
// 序列化
val modelJson = JsonUtil.toJson(model, formatted = false)

// 反序列化
val model = JsonUtil.to(modelJson, Model::class.java)
```

### 6.2 存储位置

#### 6.2.1 流水线版本表 (T_PIPELINE_RESOURCE_VERSION)

```sql
CREATE TABLE `T_PIPELINE_RESOURCE_VERSION` (
  `PROJECT_ID` varchar(64) NOT NULL,
  `PIPELINE_ID` varchar(64) NOT NULL,
  `VERSION` int(11) NOT NULL,
  `MODEL` mediumtext,           -- Model JSON字符串
  `CREATOR` varchar(64),
  `CREATE_TIME` datetime,
  PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `VERSION`)
)
```

**用途**: 存储流水线每个版本的完整 Model

#### 6.2.2 构建记录表 (T_PIPELINE_BUILD_RECORD_MODEL)

```sql
CREATE TABLE `T_PIPELINE_BUILD_RECORD_MODEL` (
  `BUILD_ID` varchar(34) NOT NULL,
  `PROJECT_ID` varchar(64) NOT NULL,
  `PIPELINE_ID` varchar(64) NOT NULL,
  `EXECUTE_COUNT` int(11) NOT NULL DEFAULT 1,
  `MODEL` mediumtext,           -- 构建运行时的 Model 快照
  PRIMARY KEY (`BUILD_ID`, `EXECUTE_COUNT`)
)
```

**用途**: 
- 存储**构建运行时的 Model 快照**
- 包含运行时状态 (status, executeCount, timeCost等)
- 支持构建详情的展示和重试

### 6.3 Model 获取流程

```kotlin
// 1. 从构建记录表获取
fun getRecordModel(
    projectId: String,
    pipelineId: String,
    version: Int,
    buildId: String,
    executeCount: Int
): Model? {
    // 从 T_PIPELINE_BUILD_RECORD_MODEL 获取记录
    val buildRecordModel = buildRecordModelDao.getRecord(
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        executeCount = executeCount
    )
    
    // 从 T_PIPELINE_RESOURCE_VERSION 获取基础模型
    val resourceStr = pipelineResourceVersionDao.getVersionModelString(
        projectId = projectId,
        pipelineId = pipelineId,
        version = version
    )
    
    // 合并基础模型和运行时数据
    val fullModel = JsonUtil.to(resourceStr, Model::class.java)
    val recordMap = JsonUtil.toMap(buildRecordModel.model)
    
    return mergeModel(fullModel, recordMap)
}
```

---

## 七、Model 在业务流程中的角色

### 7.1 流水线创建流程

```kotlin
// 1. 用户提交 Model
fun create(userId: String, model: Model): String {
    // 2. 校验 Model
    modelCheckPlugin.checkModelIntegrity(model)
    
    // 3. 生成 pipelineId
    val pipelineId = UUIDUtil.generate()
    
    // 4. 序列化 Model
    val modelJson = JsonUtil.toJson(model, formatted = false)
    
    // 5. 保存到数据库
    pipelineResourceVersionDao.create(
        projectId = projectId,
        pipelineId = pipelineId,
        version = 1,
        model = modelJson,
        creator = userId
    )
    
    return pipelineId
}
```

### 7.2 流水线启动流程

```kotlin
fun buildModel(buildInfo: BuildInfo, executeCount: Int) {
    // 1. 获取流水线最新版本的 Model
    val model = getRecordModel(
        projectId = buildInfo.projectId,
        pipelineId = buildInfo.pipelineId,
        version = buildInfo.version,
        buildId = buildInfo.buildId,
        executeCount = executeCount
    )
    
    // 2. 初始化运行时状态
    model.stages.forEach { stage ->
        stage.resetBuildOption(init = true)
        stage.containers.forEach { container ->
            container.resetBuildOption(executeCount)
            container.elements.forEach { element ->
                element.status = element.initStatus().name
            }
        }
    }
    
    // 3. 保存构建快照
    buildRecordModelDao.create(
        buildId = buildInfo.buildId,
        projectId = buildInfo.projectId,
        pipelineId = buildInfo.pipelineId,
        executeCount = executeCount,
        model = JsonUtil.toJson(model)
    )
}
```

### 7.3 构建执行引擎

```kotlin
// Stage 调度
fun scheduleStage(buildId: String, stageId: String) {
    val model = getRecordModel(buildId)
    val stage = model.getStage(stageId)
    
    if (!stage.stageEnabled()) {
        // Stage未启用，跳过
        return
    }
    
    // 检查准入条件
    if (stage.checkIn != null) {
        // 等待人工审核
        waitForReview(stage.checkIn)
    }
    
    // 调度 Containers
    stage.containers.forEach { container ->
        scheduleContainer(buildId, container)
    }
}

// Container 调度
fun scheduleContainer(buildId: String, container: Container) {
    when (container) {
        is VMBuildContainer -> {
            // 1. 启动构建机
            dispatchService.startVM(container)
            
            // 2. 执行 Elements
            container.elements.forEach { executeElement(buildId, it) }
        }
        is NormalContainer -> {
            // 直接执行 Elements
            container.elements.forEach { executeElement(buildId, it) }
        }
    }
}
```

---

## 八、Model 的版本兼容性处理

### 8.1 兼容性方法

```kotlin
// Model 层级调用
model.stages.forEach { stage ->
    stage.transformCompatibility()  // Stage级别
    stage.containers.forEach { container ->
        container.transformCompatibility()  // Container级别
        container.elements.forEach { element ->
            element.transformCompatibility()  // Element级别
        }
    }
}
```

### 8.2 典型兼容场景

#### 8.2.1 超时时间字段迁移

```kotlin
// 旧版本: timeout 是 Int 类型
// 新版本: timeoutVar 是 String 类型，支持变量

fun transformCompatibility() {
    if (additionalOptions != null && additionalOptions!!.timeoutVar.isNullOrBlank()) {
        // 迁移: 将 timeout 复制到 timeoutVar
        additionalOptions!!.timeoutVar = additionalOptions!!.timeout.toString()
    }
}
```

#### 8.2.2 废弃字段处理

```kotlin
@Deprecated("即将被timeCost代替")
var startEpoch: Long? = null

@Deprecated("即将被timeCost代替")
var elapsed: Long? = null
```

**策略**: 保留废弃字段但标记 `@Deprecated`，新逻辑使用 `timeCost`

---

## 九、最佳实践

### 9.1 创建 Model

```kotlin
// 使用默认模型
val model = Model.defaultModel(
    pipelineName = "My Pipeline",
    userId = "admin"
)

// 添加 Stage
val buildStage = Stage(
    id = "stage-2",
    name = "Build Stage",
    containers = listOf(
        VMBuildContainer(
            id = "1",
            name = "Build Job",
            baseOS = VMBaseOS.LINUX,
            elements = listOf(
                LinuxScriptElement(
                    id = "e-1",
                    name = "Compile",
                    script = "./build.sh"
                )
            )
        )
    )
)

model.stages.add(buildStage)
```

### 9.2 遍历 Model

```kotlin
fun traverseModel(model: Model, action: (Element) -> Unit) {
    model.stages.forEach { stage ->
        stage.containers.forEach { container ->
            container.elements.forEach { element ->
                action(element)
            }
        }
    }
}

// 使用
traverseModel(model) { element ->
    println("Element: ${element.name}, Type: ${element.getClassType()}")
}
```

### 9.3 修改 Model 状态

```kotlin
// 更新 Element 状态
fun updateElementStatus(model: Model, elementId: String, status: BuildStatus) {
    traverseModel(model) { element ->
        if (element.id == elementId) {
            element.status = status.name
            element.executeCount++
        }
    }
    
    // 保存到数据库
    updateModel(buildId, model)
}
```

### 9.4 统计信息

```kotlin
// 统计总任务数
val totalTasks = model.taskCount()

// 统计指定类型任务
val scriptTaskCount = model.stages.sumOf { stage ->
    stage.containers.sumOf { container ->
        container.elements.count { 
            it is LinuxScriptElement || it is WindowsScriptElement 
        }
    }
}

// 统计 Stage 数量
val stageCount = model.stages.size

// 统计 Job 数量
val jobCount = model.stages.sumOf { it.containers.size }
```

---

## 十、常见问题 (FAQ)

### Q1: Model、Stage、Container、Element 的 `id` 字段有什么区别？

**A**: 
- **Stage.id**: 系统生成，格式 `stage-{seq}`，不可编辑
- **Stage.stageIdForUser**: 用户可编辑的自定义ID
- **Container.id / containerId**: 序列ID，格式为数字字符串 `"1"`, `"2"` 等
- **Container.containerHashId**: 全局唯一HashID，用于跨构建的容器追踪
- **Container.jobId**: 用户自定义ID，用于 Job 依赖配置
- **Element.id**: 任务ID，格式 `{Stage序号}-{Container序号}-{Element序号}` (如 `"2-1-3"`)
- **Element.stepId**: 用户自定义ID，用于上下文变量引用 (如 `steps.myStep.status`)

### Q2: 为什么很多字段标记为 `var` 而不是 `val`？

**A**: 运行时需要更新这些字段的值（如 `status`、`executeCount`、`timeCost`），所以必须是可变的 (`var`)。

### Q3: `resetBuildOption()` 方法什么时候调用？

**A**:
- **重试构建**: `init = false`，保留部分历史状态
- **新构建**: `init = true`，清空所有运行时状态

### Q4: TriggerContainer 的参数如何传递到构建中？

**A**:
```kotlin
val triggerContainer = model.getTriggerContainer()
val params = triggerContainer.params

// 构建启动时，将 params 转换为构建变量
val variables = params.associate { 
    it.id to (it.value ?: it.defaultValue) 
}
```

### Q5: 如何判断一个 Stage 是否为 FinallyStage？

**A**:
```kotlin
if (stage.finally) {
    // 这是 FinallyStage，无论流水线成功或失败都会执行
}
```

### Q6: 构建矩阵如何展开？

**A**:
```kotlin
// 1. 父容器配置 matrixControlOption
val parentContainer = VMBuildContainer(
    matrixControlOption = MatrixControlOption(
        strategyStr = """{"os": ["linux", "windows"], "version": ["1.0", "2.0"]}"""
    )
)

// 2. 引擎解析策略并生成子容器
val childContainers = matrixService.expand(parentContainer)
// 生成: (linux,1.0), (linux,2.0), (windows,1.0), (windows,2.0)

// 3. 设置父容器字段
parentContainer.groupContainers = childContainers.toMutableList()
parentContainer.matrixGroupFlag = true

// 4. 设置子容器字段
childContainers.forEach { child ->
    child.matrixGroupId = parentContainer.containerHashId
    child.matrixContext = mapOf("os" to "linux", "version" to "1.0") // 示例
}
```

### Q7: Post 任务如何实现？

**A**: 通过 `ElementAdditionalOptions.elementPostInfo` 配置：

```kotlin
val mainElement = LinuxScriptElement(
    id = "e-1",
    name = "Main Task",
    script = "./build.sh"
)

val postElement = LinuxScriptElement(
    id = "e-2",
    name = "Cleanup",
    script = "./cleanup.sh",
    additionalOptions = ElementAdditionalOptions(
        elementPostInfo = ElementPostInfo(
            parentElementId = "e-1",       // 关联主任务
            postCondition = "failure"      // 失败时执行
        )
    )
)

// Post任务会在主任务执行完毕后根据条件执行
```

---

## 十一、检查清单

在操作 Model 时，请确认以下事项：

- [ ] **结构完整性**: Model 至少包含一个 Stage，第一个 Stage 必须包含 TriggerContainer
- [ ] **ID 唯一性**: 同一层级的 `id` 字段不能重复
- [ ] **FinallyStage 唯一**: 最多只能有一个 FinallyStage，且必须在最后位置
- [ ] **容器类型匹配**: 第一个 Stage 的第一个 Container 必须是 TriggerContainer
- [ ] **序列化兼容**: 使用 Jackson 的 `@JsonTypeInfo` 确保多态序列化正确
- [ ] **运行时字段**: 编排阶段不要设置运行时字段（如 `status`、`executeCount`）
- [ ] **版本兼容**: 调用 `transformCompatibility()` 处理历史数据
- [ ] **Job 依赖**: 检查 `JobControlOption.dependOnId` 中的 JobID 是否存在
- [ ] **流程控制**: 确认 `enable` 字段、`runCondition` 等流程控制选项配置正确
- [ ] **超时配置**: 优先使用 `timeoutVar` 而非 `timeout`，支持变量表达式
- [ ] **矩阵配置**: 矩阵父容器不应有实际任务，任务应在子容器中
- [ ] **参数验证**: TriggerContainer 的 `params` 字段验证 `required`、`valueNotEmpty` 等约束

---

## 十二、相关文件索引

### 核心模型文件
```
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/
├── Model.kt                           # Model 顶层定义
├── container/
│   ├── Stage.kt                       # Stage 定义
│   ├── Container.kt                   # Container 接口
│   ├── TriggerContainer.kt            # 触发容器
│   ├── VMBuildContainer.kt            # 虚拟机构建容器
│   ├── NormalContainer.kt             # 无编译环境容器
│   └── JobTemplateContainer.kt        # Job模板容器
├── pojo/
│   ├── element/
│   │   ├── Element.kt                 # Element 抽象类
│   │   ├── ElementAdditionalOptions.kt # 插件流程控制
│   │   ├── agent/                     # 有编译环境插件
│   │   ├── trigger/                   # 触发器插件
│   │   └── market/                    # 市场插件
│   ├── BuildFormProperty.kt           # 流水线参数定义
│   └── StagePauseCheck.kt             # Stage准入准出
└── option/
    ├── JobControlOption.kt            # Job流程控制
    └── StageControlOption.kt          # Stage流程控制
```

### 业务逻辑文件
```
src/backend/ci/core/process/
├── biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/
│   ├── record/
│   │   ├── BaseBuildRecordService.kt      # Model获取和保存
│   │   └── PipelineBuildRecordService.kt  # 流水线构建记录
│   └── PipelineBuildDetailService.kt      # 构建详情服务
├── biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/
│   ├── BuildStartControl.kt               # 构建启动控制
│   └── BuildEndControl.kt                 # 构建结束控制
└── biz-process/src/main/kotlin/com/tencent/devops/process/service/
    └── PipelineRepositoryService.kt       # 流水线仓库服务
```

### 数据库DAO
```
src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/dao/
├── PipelineResourceVersionDao.kt          # 流水线版本表
└── PipelineBuildRecordModelDao.kt         # 构建记录表
```

---

## 十三、辅助数据结构详解

本章详细介绍 Model 中使用的各类辅助数据结构。

### 13.1 DispatchType (构建机调度类型)

DispatchType 是一个**抽象类**，定义了构建机的调度方式：

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "buildType")
@JsonSubTypes(
    JsonSubTypes.Type(value = DockerDispatchType::class, name = "DOCKER"),
    JsonSubTypes.Type(value = KubernetesDispatchType::class, name = "KUBERNETES"),
    JsonSubTypes.Type(value = ThirdPartyAgentIDDispatchType::class, name = "THIRD_PARTY_AGENT_ID"),
    JsonSubTypes.Type(value = ThirdPartyAgentEnvDispatchType::class, name = "THIRD_PARTY_AGENT_ENV"),
    JsonSubTypes.Type(value = ThirdPartyDevCloudDispatchType::class, name = "THIRD_PARTY_DEVCLOUD")
)
abstract class DispatchType(
    open var value: String,                           // 调度值（如镜像名、AgentID等）
    open val routeKeySuffix: DispatchRouteKeySuffix?  // 路由键后缀
) {
    // 替换变量
    fun replaceVariable(variables: Map<String, String>) {
        value = EnvUtils.parseEnv(value, variables)
        replaceField(variables)
    }
    
    // 获取构建类型
    abstract fun buildType(): BuildType
    
    // 替换自定义字段
    protected abstract fun replaceField(variables: Map<String, String>)
    
    // 保存前清理数据
    abstract fun cleanDataBeforeSave()
}
```

**调度类型说明**:

| 类型 | 说明 | 使用场景 |
|-----|------|---------|
| `DOCKER` | Docker 容器调度 | 公共构建机、Docker 镜像构建 |
| `KUBERNETES` | Kubernetes 调度 | K8s 集群构建 |
| `THIRD_PARTY_AGENT_ID` | 第三方构建机（指定ID） | 指定特定构建机 |
| `THIRD_PARTY_AGENT_ENV` | 第三方构建机（环境） | 从环境池中选择构建机 |
| `THIRD_PARTY_DEVCLOUD` | 云开发机 | 云桌面/远程开发环境 |

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/type/DispatchType.kt`

### 13.2 MutexGroup (互斥组)

互斥组用于控制**同一时刻只有一个 Job 可以执行**：

```kotlin
data class MutexGroup(
    val enable: Boolean,                    // 是否启用
    val mutexGroupName: String? = "",       // 互斥组名称
    val queueEnable: Boolean,               // 是否启用排队
    var timeout: Int = 0,                   // 排队等待超时（分钟），0表示不等待直接失败
    var timeoutVar: String? = null,         // 超时变量（支持表达式）
    val queue: Int = 0,                     // 排队队列大小
    var runtimeMutexGroup: String? = null,  // 运行时实际互斥锁名称
    var linkTip: String? = null             // 占用锁定的信息提示
) {
    // 获取运行时互斥组名称
    fun fetchRuntimeMutexGroup() = runtimeMutexGroup ?: mutexGroupName ?: ""
    
    // 生成互斥锁 Redis Key
    fun genMutexLockKey(projectId: String): String {
        val mutexGroupName = fetchRuntimeMutexGroup()
        return "lock:container:mutex:$projectId:$mutexGroupName:lock"
    }
    
    // 生成排队 Redis Key
    fun genMutexQueueKey(projectId: String): String {
        val mutexGroupName = fetchRuntimeMutexGroup()
        return "lock:container:mutex:$projectId:$mutexGroupName:queue"
    }
}
```

**使用场景**:
- 数据库部署：同一时刻只能有一个部署任务
- 资源竞争：多流水线共享同一资源
- 顺序执行：确保任务按顺序执行

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/container/MutexGroup.kt`

### 13.3 MatrixControlOption (构建矩阵配置)

构建矩阵允许一个 Job 根据参数组合**分裂成多个并行子 Job**：

```kotlin
data class MatrixControlOption(
    val strategyStr: String? = null,        // 分裂策略（JSON/YAML格式）
    val includeCaseStr: String? = null,     // 额外包含的参数组合
    val excludeCaseStr: String? = null,     // 排除的参数组合
    val fastKill: Boolean? = false,         // 失败快速终止整个矩阵
    var maxConcurrency: Int? = 5,           // 最大并发数
    var customDispatchInfo: DispatchInfo?,  // 自定义调度信息
    var totalCount: Int? = null,            // 矩阵总数量（运行时计算）
    var finishCount: Int? = null            // 已完成数量（运行时计算）
) {
    companion object {
        const val MATRIX_CASE_MAX_COUNT = 256  // 矩阵最大组合数
    }
    
    // 将配置转换为矩阵配置对象
    fun convertMatrixConfig(buildContext: Map<String, String>): MatrixConfig {
        // 解析 strategyStr（支持 YAML 和 JSON 格式）
        // 解析 includeCaseStr 和 excludeCaseStr
        // 返回 MatrixConfig 对象
    }
}
```

**矩阵策略示例**:

```yaml
# YAML 格式
strategy:
  os: [linux, windows, macos]
  node: [14, 16, 18]
include:
  - os: linux
    node: 20
exclude:
  - os: macos
    node: 14
```

```json
// JSON 格式
{
  "os": ["linux", "windows"],
  "version": ["1.0", "2.0"]
}
```

**矩阵展开流程**:
1. 解析 `strategyStr` 生成笛卡尔积
2. 添加 `includeCaseStr` 中的额外组合
3. 移除 `excludeCaseStr` 中的排除组合
4. 生成子容器列表

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/option/MatrixControlOption.kt`

### 13.4 StagePauseCheck (Stage 准入准出配置)

Stage 准入准出用于在 Stage 执行前后进行**人工审核或质量红线检查**：

```kotlin
data class StagePauseCheck(
    var manualTrigger: Boolean? = false,           // 是否人工触发
    var status: String? = null,                    // 审核状态
    var reviewDesc: String? = null,                // 审核说明
    var reviewGroups: MutableList<StageReviewGroup>?, // 审核流配置
    var reviewParams: List<ManualReviewParam>?,    // 审核变量
    var timeout: Int? = 24,                        // 审核超时（小时）
    var ruleIds: List<String>? = null,             // 质量红线规则ID
    var checkTimes: Int? = null,                   // 质量红线检查次数
    var markdownContent: Boolean? = false,         // 是否 Markdown 格式
    var notifyType: MutableList<String>?,          // 通知类型
    var notifyGroup: MutableList<String>?          // 企业微信群ID
) {
    // 获取当前等待审核的组
    fun groupToReview(): StageReviewGroup? { ... }
    
    // 判断用户是否在审核人名单中
    fun reviewerContains(userId: String): Boolean { ... }
    
    // 审核通过/驳回
    fun reviewGroup(
        userId: String,
        action: ManualReviewAction,
        groupId: String?,
        params: List<ManualReviewParam>?,
        suggest: String?
    ): StageReviewGroup? { ... }
    
    // 初始化审核组ID
    fun fixReviewGroups(init: Boolean) { ... }
    
    // 替换审核人变量
    fun parseReviewVariables(variables: Map<String, String>, dialect: IPipelineDialect) { ... }
    
    // 重试时重置状态
    fun retryRefresh() { ... }
}
```

**StageReviewGroup (审核组)**:

```kotlin
data class StageReviewGroup(
    var id: String? = null,                  // 审核组ID（后台生成）
    val name: String = "Flow 1",             // 审核组名称
    var reviewers: List<String> = listOf(),  // 审核人员列表
    var groups: List<String> = listOf(),     // 审核用户组
    var status: String? = null,              // 审核结果（PROCESS/ABORT）
    var operator: String? = null,            // 审核操作人
    var reviewTime: Long? = null,            // 审核时间
    var suggest: String? = null,             // 审核建议
    var params: List<ManualReviewParam>?     // 审核传入变量
)
```

**审核流程**:
```
Stage 开始
    ↓
checkIn 不为空？ ──是──→ 等待审核
    │                      ↓
    否                 审核通过？ ──否──→ 终止 Stage
    ↓                      │
执行 Containers           是
    ↓                      ↓
checkOut 不为空？ ──是──→ 等待审核
    │                      ↓
    否                 审核通过？ ──否──→ 标记失败
    ↓                      │
Stage 结束                是
                          ↓
                     Stage 结束
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/StagePauseCheck.kt`

### 13.5 BuildNo (构建版本号)

BuildNo 用于管理流水线的**构建版本号**：

```kotlin
data class BuildNo(
    var buildNo: Int,                    // 构建号初始值
    val buildNoType: BuildNoType,        // 构建号类型
    var required: Boolean? = false,      // 是否必填
    var currentBuildNo: Int? = null      // 当前最新值
)

enum class BuildNoType {
    CONSISTENT,                  // 固定值（不自增）
    SUCCESS_BUILD_INCREMENT,     // 成功构建后自增
    EVERY_BUILD_INCREMENT        // 每次构建自增
}
```

**使用场景**:
- `CONSISTENT`: 版本号由用户手动指定
- `SUCCESS_BUILD_INCREMENT`: 只有构建成功才递增版本号
- `EVERY_BUILD_INCREMENT`: 每次触发构建都递增版本号

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/BuildNo.kt`

### 13.6 BuildRecordTimeCost (耗时统计)

BuildRecordTimeCost 用于记录各层级的**执行耗时**：

```kotlin
data class BuildRecordTimeCost(
    var systemCost: Long = 0,    // 系统耗时（由总耗时减去其他得出）
    var executeCost: Long = 0,   // 执行耗时
    var waitCost: Long = 0,      // 等待耗时（排队+人工审核）
    var queueCost: Long = 0,     // 排队耗时（并发/互斥组）
    var totalCost: Long = 0      // 总耗时（结束时间-开始时间）
)
```

**耗时计算公式**:
```
totalCost = systemCost + executeCost + waitCost
waitCost >= queueCost  // waitCost 包含 queueCost
```

**应用层级**:
- **Model 级别**: 整个流水线的耗时
- **Stage 级别**: 单个阶段的耗时
- **Container 级别**: 单个 Job 的耗时
- **Element 级别**: 单个插件的耗时

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/time/BuildRecordTimeCost.kt`

### 13.7 ElementPostInfo (Post 任务信息)

ElementPostInfo 用于配置**Post 任务**（在主任务执行后执行的清理任务）：

```kotlin
data class ElementPostInfo(
    val postEntryParam: String,        // 入口参数
    val postCondition: String,         // 执行条件（always/success/failure）
    var parentElementId: String,       // 父元素ID
    val parentElementName: String,     // 父元素名称
    val parentElementJobIndex: Int     // 父元素在 Job 中的位置
)
```

**执行条件**:
- `always`: 无论主任务成功或失败都执行
- `success`: 只有主任务成功才执行
- `failure`: 只有主任务失败才执行

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/ElementPostInfo.kt`

---

## 十四、BuildStatus (构建状态) 详解

BuildStatus 是一个**核心枚举**，定义了构建过程中所有可能的状态：

```kotlin
enum class BuildStatus(val statusName: String, val visible: Boolean) {
    // 最终态 - 成功
    SUCCEED("succeed", true),                    // 0 成功
    SKIP("skip", true),                          // 11 跳过
    REVIEW_PROCESSED("reviewProcessed", true),   // 7 审核通过
    QUALITY_CHECK_PASS("qualityCheckPass", true),// 25 质量红线通过
    STAGE_SUCCESS("stageSuccess", true),         // 22 Stage成功（人工取消后）
    
    // 最终态 - 失败
    FAILED("failed", true),                      // 1 失败
    CANCELED("canceled", true),                  // 2 取消
    TERMINATE("terminate", true),                // 4 终止
    REVIEW_ABORT("reviewAbort", true),           // 6 审核驳回
    HEARTBEAT_TIMEOUT("heartbeatTimeout", true), // 8 心跳超时
    QUALITY_CHECK_FAIL("qualityCheckFail", true),// 12 质量红线失败
    QUEUE_TIMEOUT("queueTimeout", true),         // 17 排队超时
    EXEC_TIMEOUT("execTimeout", true),           // 18 执行超时
    QUOTA_FAILED("quotaFailed", true),           // 23 配额失败
    
    // 中间态 - 运行中
    RUNNING("running", true),                    // 3 运行中
    REVIEWING("reviewing", true),                // 5 审核中
    PREPARE_ENV("prepareEnv", true),             // 9 准备环境中
    LOOP_WAITING("loopWaiting", true),           // 14 轮循等待（互斥组）
    CALL_WAITING("callWaiting", true),           // 15 等待回调
    PAUSE("pause", true),                        // 21 暂停执行
    DEPENDENT_WAITING("dependentWaiting", true), // 24 依赖等待
    QUALITY_CHECK_WAIT("qualityCheckWait", true),// 26 质量红线等待
    TRIGGER_REVIEWING("triggerReviewing", true), // 27 触发待审核
    
    // 初始态
    QUEUE("queue", true),                        // 13 排队
    QUEUE_CACHE("queueCache", true),             // 19 队列待处理（瞬态）
    RETRY("retry", true),                        // 20 重试
    
    // 不可见状态
    UNEXEC("unexec", false),                     // 10 从未执行
    TRY_FINALLY("tryFinally", false),            // 16 后台状态
    UNKNOWN("unknown", false);                   // 99 未知
    
    // 状态判断方法
    fun isNeverRun(): Boolean = this == UNEXEC || this == TRIGGER_REVIEWING
    fun isFinish(): Boolean = isFailure() || isSuccess() || isCancel()
    fun isFailure(): Boolean = this == FAILED || isPassiveStop() || isTimeout()
    fun isSuccess(): Boolean = this == SUCCEED || this == SKIP || this == REVIEW_PROCESSED
    fun isCancel(): Boolean = this == CANCELED
    fun isRunning(): Boolean = this == RUNNING || this == LOOP_WAITING || ...
    fun isReadyToRun(): Boolean = this == QUEUE || this == QUEUE_CACHE || this == RETRY
    fun isPause(): Boolean = this == PAUSE
    fun isTimeout(): Boolean = this == QUEUE_TIMEOUT || this == EXEC_TIMEOUT || ...
}
```

**状态转换图**:

```
                    ┌─────────────────────────────────────────────────────┐
                    │                                                     │
                    ▼                                                     │
QUEUE ──→ QUEUE_CACHE ──→ PREPARE_ENV ──→ RUNNING ──→ SUCCEED           │
  │           │               │              │           │               │
  │           │               │              │           ▼               │
  │           │               │              │       STAGE_SUCCESS       │
  │           │               │              │                           │
  │           │               │              ├──→ FAILED ────────────────┤
  │           │               │              │                           │
  │           │               │              ├──→ CANCELED ──────────────┤
  │           │               │              │                           │
  │           │               │              ├──→ EXEC_TIMEOUT ──────────┤
  │           │               │              │                           │
  │           │               │              ├──→ PAUSE ──→ RUNNING      │
  │           │               │              │                           │
  │           │               │              └──→ REVIEWING ──→ REVIEW_PROCESSED
  │           │               │                       │                  │
  │           │               │                       └──→ REVIEW_ABORT ─┤
  │           │               │                                          │
  │           │               └──→ HEARTBEAT_TIMEOUT ────────────────────┤
  │           │                                                          │
  │           └──→ DEPENDENT_WAITING ──→ PREPARE_ENV                     │
  │                                                                      │
  └──→ QUEUE_TIMEOUT ────────────────────────────────────────────────────┘
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/BuildStatus.kt`

---

## 十五、Element 子类型详解

### 15.1 ManualTriggerElement (手动触发)

```kotlin
data class ManualTriggerElement(
    override val name: String = "手动触发",
    override var id: String? = null,
    override var status: String? = null,
    override var stepId: String? = null,
    var canElementSkip: Boolean? = false,      // 是否可跳过插件
    var useLatestParameters: Boolean? = false, // 使用最近一次参数
    var buildMsg: String? = null               // 默认构建信息
) : Element(name, id, status) {
    companion object {
        const val classType = "manualTrigger"
    }
    
    // 支持的启动类型
    private val startTypeSet = setOf(
        StartType.MANUAL.name,    // 手动触发
        StartType.SERVICE.name,   // 服务调用
        StartType.PIPELINE.name   // 子流水线调用
    )
}
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/trigger/ManualTriggerElement.kt`

### 15.2 TimerTriggerElement (定时触发)

```kotlin
data class TimerTriggerElement(
    override val name: String = "定时触发",
    override var id: String? = null,
    override var status: String? = null,
    override var version: String = "1.*",
    override var stepId: String? = null,
    
    @Deprecated("使用 advanceExpression")
    val expression: String? = null,            // 旧版 Cron 表达式
    val newExpression: List<String>? = null,   // 新版 Cron 表达式列表
    val advanceExpression: List<String>? = null, // 高级表达式（支持变量）
    
    val noScm: Boolean? = false,               // 代码未更新不触发
    val branches: List<String>? = null,        // 指定分支
    val repositoryType: TriggerRepositoryType?, // 代码库类型
    val repoHashId: String? = null,            // 代码库 HashId
    val repoName: String? = null,              // 代码库别名
    val startParams: String? = null            // 启动参数 JSON
) : Element(name, id, status) {
    companion object {
        const val classType = "timerTrigger"
    }
    
    // 转换 Cron 表达式（Unix → Quartz）
    fun convertExpressions(params: Map<String, String>): Set<String> { ... }
    
    // 解析启动参数
    fun convertStartParams(): Map<String, String>? { ... }
}
```

**Cron 表达式格式**:
- Unix 格式: `0 0 * * *` (分 时 日 月 周)
- Quartz 格式: `0 0 0 * * ?` (秒 分 时 日 月 周)

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/trigger/TimerTriggerElement.kt`

### 15.3 LinuxScriptElement (Linux 脚本)

```kotlin
data class LinuxScriptElement(
    override val name: String = "执行Linux脚本",
    override var id: String? = null,
    override var status: String? = null,
    override var stepId: String? = null,
    override var customEnv: List<NameAndValue>? = null,
    
    val errorFAQUrl: String? = null,           // FAQ 链接
    val scriptType: BuildScriptType,           // 脚本类型（SHELL）
    val script: String,                        // 脚本内容
    val continueNoneZero: Boolean?,            // 非0退出码继续执行
    val enableArchiveFile: Boolean? = false,   // 启用失败归档
    val archiveFile: String? = null,           // 归档文件路径
    override var additionalOptions: ElementAdditionalOptions? = null
) : Element(name, id, status, additionalOptions = additionalOptions) {
    companion object {
        const val classType = "linuxScript"
    }
    
    // 生成任务参数（URL 编码脚本）
    override fun genTaskParams(): MutableMap<String, Any> {
        val mutableMap = super.genTaskParams()
        mutableMap["script"] = URLEncoder.encode(script, "UTF-8")
        return mutableMap
    }
}
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/agent/LinuxScriptElement.kt`

### 15.4 MarketBuildAtomElement (研发商店插件)

```kotlin
data class MarketBuildAtomElement(
    override val name: String = "任务名称由用户自己填写",
    override var id: String? = null,
    override var status: String? = null,
    var atomCode: String = "",                 // 插件唯一标识
    override var version: String = "1.*",      // 插件版本
    override var stepId: String? = null,
    override var customEnv: List<NameAndValue>? = null,
    var data: Map<String, Any> = mapOf(),      // 插件参数数据
    override var additionalOptions: ElementAdditionalOptions? = null
) : Element(name, id, status, additionalOptions = additionalOptions) {
    companion object {
        const val classType = "marketBuild"
    }
    
    override fun getAtomCode(): String = atomCode
    
    // 转换为 YAML 格式
    override fun transferYaml(defaultValue: JSONObject?): PreStep {
        val input = data["input"] as Map<String, Any>? ?: emptyMap()
        return PreStep(
            name = name,
            id = stepId,
            uses = "${getAtomCode()}@$version",
            namespace = data["namespace"]?.toString()?.ifBlank { null },
            with = TransferUtil.simplifyParams(defaultValue, input).ifEmpty { null }
        )
    }
}
```

**data 字段结构**:
```kotlin
data = mapOf(
    "input" to mapOf(          // 插件输入参数
        "param1" to "value1",
        "param2" to "value2"
    ),
    "output" to mapOf(         // 插件输出参数
        "result" to "string"
    ),
    "namespace" to "myNamespace" // 命名空间
)
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/market/MarketBuildAtomElement.kt`

---

## 十六、BuildFormPropertyType (参数类型) 详解

BuildFormPropertyType 定义了流水线参数的**所有可用类型**：

```kotlin
enum class BuildFormPropertyType(val value: String) {
    STRING("string"),              // 字符串
    TEXTAREA("textarea"),          // 多行文本
    ENUM("enum"),                  // 枚举（下拉单选）
    DATE("date"),                  // 日期
    LONG("long"),                  // 长整型
    BOOLEAN("boolean"),            // 布尔值
    SVN_TAG("svn_tag"),            // SVN Tag
    GIT_REF("git_ref"),            // Git 引用（分支/Tag）
    REPO_REF("repo_ref"),          // 代码库引用
    MULTIPLE("multiple"),          // 多选
    CODE_LIB("code_lib"),          // 代码库
    CONTAINER_TYPE("container_type"), // 构建机类型
    ARTIFACTORY("artifactory"),    // 版本仓库
    SUB_PIPELINE("sub_pipeline"),  // 子流水线
    CUSTOM_FILE("custom_file"),    // 自定义仓库文件
    PASSWORD("password"),          // 密码（加密存储）
    TEMPORARY("do not storage")    // 临时参数（不存储）
}
```

**各类型说明**:

| 类型 | 前端组件 | 说明 |
|-----|---------|------|
| `STRING` | 单行输入框 | 普通字符串参数 |
| `TEXTAREA` | 多行输入框 | 长文本参数 |
| `ENUM` | 下拉选择框 | 需配合 `options` 字段 |
| `BOOLEAN` | 开关/复选框 | true/false |
| `GIT_REF` | 分支选择器 | 从代码库获取分支列表 |
| `CODE_LIB` | 代码库选择器 | 选择项目下的代码库 |
| `SUB_PIPELINE` | 流水线选择器 | 选择项目下的流水线 |
| `PASSWORD` | 密码输入框 | 值会加密存储 |

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/BuildFormPropertyType.kt`

---

## 十七、VMBaseOS (操作系统类型)

VMBaseOS 定义了构建机支持的**操作系统类型**：

```kotlin
enum class VMBaseOS {
    MACOS,    // macOS 系统
    LINUX,    // Linux 系统
    WINDOWS,  // Windows 系统
    ALL       // 所有系统（用于无编译环境）
}
```

**使用场景**:
- `VMBuildContainer.baseOS`: 指定 Job 运行的操作系统
- 构建机调度时根据 `baseOS` 选择合适的构建机

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/VMBaseOS.kt`

---

## 十八、运行条件枚举详解

### 18.1 JobRunCondition (Job 运行条件)

```kotlin
enum class JobRunCondition {
    STAGE_RUNNING,              // 当前 Stage 开始运行时（默认）
    CUSTOM_VARIABLE_MATCH,      // 自定义变量全部满足时运行
    CUSTOM_VARIABLE_MATCH_NOT_RUN, // 自定义变量全部满足时不运行
    CUSTOM_CONDITION_MATCH,     // 满足自定义条件表达式时运行
    PREVIOUS_STAGE_SUCCESS,     // 上游 Stage 成功时
    PREVIOUS_STAGE_FAILED,      // 上游 Stage 失败时
    PREVIOUS_STAGE_CANCEL       // 上游 Stage 取消时
}
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/JobRunCondition.kt`

### 18.2 StageRunCondition (Stage 运行条件)

```kotlin
enum class StageRunCondition {
    AFTER_LAST_FINISHED,        // 上个阶段执行结束（默认）
    CUSTOM_VARIABLE_MATCH,      // 自定义变量全部满足时运行
    CUSTOM_VARIABLE_MATCH_NOT_RUN, // 自定义变量全部满足时不运行
    CUSTOM_CONDITION_MATCH      // 满足自定义条件表达式时运行
}
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/StageRunCondition.kt`

### 18.3 RunCondition (Element 运行条件)

```kotlin
enum class RunCondition {
    PRE_TASK_SUCCESS,              // 所有前置插件运行成功时
    PRE_TASK_FAILED_BUT_CANCEL,    // 前置失败也运行（除非被取消）
    PRE_TASK_FAILED_EVEN_CANCEL,   // 前置失败也运行（即使被取消）
    PRE_TASK_FAILED_ONLY,          // 只有前置失败才运行
    CUSTOM_VARIABLE_MATCH,         // 自定义变量全部满足时运行
    CUSTOM_VARIABLE_MATCH_NOT_RUN, // 自定义变量全部满足时不运行
    PARENT_TASK_CANCELED_OR_TIMEOUT, // 父任务取消或超时时运行
    PARENT_TASK_FINISH             // 父任务结束就运行
}
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/ElementAdditionalOptions.kt`

---

## 十九、Model 校验机制

### 19.1 ModelCheckPlugin 接口

ModelCheckPlugin 是 Model 校验的**核心扩展点**：

```kotlin
interface ModelCheckPlugin {
    // 检查 Model 完整性，返回元素数量
    fun checkModelIntegrity(
        model: Model,
        projectId: String?,
        userId: String,
        isTemplate: Boolean = false,
        oauthUser: String? = null,
        pipelineDialect: IPipelineDialect? = null,
        pipelineId: String = ""
    ): Int
    
    // 检查 Setting 完整性
    fun checkSettingIntegrity(setting: PipelineSetting, projectId: String?)
    
    // 清理 Model
    fun clearUpModel(model: Model)
    
    // 删除 Element 前的处理
    fun beforeDeleteElementInExistsModel(
        existModel: Model,
        sourceModel: Model? = null,
        param: BeforeDeleteParam
    )
    
    // 检查 Element 超时配置
    fun checkElementTimeoutVar(container: Container, element: Element, contextMap: Map<String, String>)
    
    // 检查互斥组配置
    fun checkMutexGroup(container: Container, contextMap: Map<String, String>)
    
    // 检查 Job 运行条件
    fun checkJobCondition(container: Container, finallyStage: Boolean, contextMap: Map<String, String>)
}
```

**文件位置**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/extend/ModelCheckPlugin.kt`

### 19.2 DefaultModelCheckPlugin 实现

DefaultModelCheckPlugin 是默认的校验实现：

```kotlin
open class DefaultModelCheckPlugin(
    open val client: Client,
    open val pipelineCommonSettingConfig: PipelineCommonSettingConfig,
    open val stageCommonSettingConfig: StageCommonSettingConfig,
    open val jobCommonSettingConfig: JobCommonSettingConfig,
    open val taskCommonSettingConfig: TaskCommonSettingConfig,
    open val elementBizPluginServices: List<IElementBizPluginService>
) : ModelCheckPlugin {
    
    override fun checkModelIntegrity(model: Model, ...): Int {
        var metaSize = 0
        
        // 1. 检查流水线名称
        PipelineUtils.checkPipelineName(model.name, maxSize)
        
        // 2. 检查流水线描述长度
        PipelineUtils.checkPipelineDescLength(model.desc, maxSize)
        
        // 3. 检查 Model JSON 大小
        val modelSize = JsonUtil.toJson(model).length
        if (modelSize > maxModelSize) {
            throw ErrorCodeException(ERROR_PIPELINE_MODEL_TOO_LARGE)
        }
        
        // 4. 检查 Stage 数量
        if (stages.size > maxStageNum) {
            throw ErrorCodeException(ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE)
        }
        
        // 5. 检查触发容器
        checkTriggerContainer(trigger)
        
        // 6. 遍历检查每个 Stage
        model.stages.forEachIndexed { index, stage ->
            // 检查 Container 数量
            // 检查 FinallyStage 位置
            // 检查审核组配置
            // 检查运行条件
            // 检查 Element
            metaSize += stage.checkJob(...)
        }
        
        return metaSize
    }
}
```

**校验项清单**:
1. 流水线名称长度（默认最大 64 字符）
2. 流水线描述长度（默认最大 100 字符）
3. Model JSON 大小（默认最大 4MB）
4. Stage 数量（默认最大 20 个）
5. 每个 Stage 下的 Job 数量（默认最大 20 个）
6. 每个 Job 下的 Element 数量（默认最大 50 个）
7. FinallyStage 必须在最后位置
8. 触发容器必须存在且在第一个位置
9. 超时时间配置合法性
10. 互斥组配置合法性
11. 运行条件表达式长度

**文件位置**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/extend/DefaultModelCheckPlugin.kt`

---

## 二十、Element 目录结构

Element 子类按功能分类存放在不同目录：

```
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/
├── Element.kt                        # 抽象基类
├── ElementAdditionalOptions.kt       # 附加选项
├── ElementBaseInfo.kt                # 基础信息
├── ElementPostInfo.kt                # Post 任务信息
├── ElementProp.kt                    # 属性
├── EmptyElement.kt                   # 空元素（默认）
├── StepTemplateElement.kt            # Step 模板
├── SubPipelineCallElement.kt         # 子流水线调用
│
├── agent/                            # 有编译环境插件
│   ├── CodeGitElement.kt             # Git 拉代码
│   ├── CodeGitlabElement.kt          # GitLab 拉代码
│   ├── CodeSvnElement.kt             # SVN 拉代码
│   ├── GithubElement.kt              # GitHub 拉代码
│   ├── LinuxScriptElement.kt         # Linux 脚本
│   ├── WindowsScriptElement.kt       # Windows 脚本
│   └── ManualReviewUserTaskElement.kt # 人工审核
│
├── trigger/                          # 触发器
│   ├── ManualTriggerElement.kt       # 手动触发
│   ├── TimerTriggerElement.kt        # 定时触发
│   ├── RemoteTriggerElement.kt       # 远程触发
│   ├── CodeGitWebHookTriggerElement.kt    # Git WebHook
│   ├── CodeGitlabWebHookTriggerElement.kt # GitLab WebHook
│   ├── CodeGithubWebHookTriggerElement.kt # GitHub WebHook
│   ├── CodeSVNWebHookTriggerElement.kt    # SVN WebHook
│   ├── CodeTGitWebHookTriggerElement.kt   # TGit WebHook
│   ├── CodeP4WebHookTriggerElement.kt     # P4 WebHook
│   └── WebHookTriggerElement.kt      # WebHook 基类
│
├── market/                           # 研发商店插件
│   ├── MarketBuildAtomElement.kt     # 有编译环境插件
│   ├── MarketBuildLessAtomElement.kt # 无编译环境插件
│   ├── MarketCheckImageElement.kt    # 镜像检查
│   └── AtomBuildArchiveElement.kt    # 构建归档
│
├── quality/                          # 质量红线
│   ├── QualityGateInElement.kt       # 准入质量红线
│   └── QualityGateOutElement.kt      # 准出质量红线
│
├── matrix/                           # 构建矩阵
│   └── MatrixStatusElement.kt        # 矩阵状态元素
│
└── atom/                             # 原子相关
    ├── BeforeDeleteParam.kt          # 删除前参数
    ├── ElementBatchCheckParam.kt     # 批量检查参数
    ├── ElementCheckResult.kt         # 检查结果
    ├── ElementHolder.kt              # 元素持有者
    ├── ManualReviewParam.kt          # 人工审核参数
    ├── ManualReviewParamPair.kt      # 审核参数对
    ├── ManualReviewParamType.kt      # 审核参数类型
    └── SubPipelineType.kt            # 子流水线类型
```

---

## 二十一、完整 JSON 示例

### 21.1 最小化 Model

```json
{
  "name": "My Pipeline",
  "desc": "A simple pipeline",
  "stages": [
    {
      "@type": "stage",
      "id": "stage-1",
      "name": "Trigger Stage",
      "containers": [
        {
          "@type": "trigger",
          "id": "0",
          "name": "trigger",
          "elements": [
            {
              "@type": "manualTrigger",
              "id": "T-1-1-1",
              "name": "手动触发"
            }
          ],
          "params": []
        }
      ]
    }
  ]
}
```

### 21.2 完整 Model 示例

```json
{
  "name": "Full Pipeline Example",
  "desc": "A complete pipeline with all features",
  "stages": [
    {
      "@type": "stage",
      "id": "stage-1",
      "name": "Trigger",
      "containers": [
        {
          "@type": "trigger",
          "id": "0",
          "name": "trigger",
          "elements": [
            {
              "@type": "manualTrigger",
              "id": "T-1-1-1",
              "name": "手动触发",
              "canElementSkip": false,
              "useLatestParameters": false
            },
            {
              "@type": "timerTrigger",
              "id": "T-1-1-2",
              "name": "定时触发",
              "advanceExpression": ["0 0 8 * * ?"],
              "noScm": false
            }
          ],
          "params": [
            {
              "id": "version",
              "name": "版本号",
              "type": "STRING",
              "required": true,
              "defaultValue": "1.0.0",
              "desc": "发布版本号"
            },
            {
              "id": "env",
              "name": "环境",
              "type": "ENUM",
              "required": true,
              "defaultValue": "dev",
              "options": [
                {"key": "dev", "value": "开发环境"},
                {"key": "test", "value": "测试环境"},
                {"key": "prod", "value": "生产环境"}
              ]
            }
          ],
          "buildNo": {
            "buildNo": 1,
            "buildNoType": "EVERY_BUILD_INCREMENT",
            "required": false
          }
        }
      ]
    },
    {
      "@type": "stage",
      "id": "stage-2",
      "name": "Build",
      "stageControlOption": {
        "enable": true,
        "runCondition": "AFTER_LAST_FINISHED"
      },
      "containers": [
        {
          "@type": "vmBuild",
          "id": "1",
          "name": "Build Job",
          "baseOS": "LINUX",
          "dispatchType": {
            "buildType": "DOCKER",
            "value": "bkci/ci:latest"
          },
          "jobControlOption": {
            "enable": true,
            "timeout": 60,
            "runCondition": "STAGE_RUNNING"
          },
          "elements": [
            {
              "@type": "linuxScript",
              "id": "e-2-1-1",
              "name": "编译",
              "scriptType": "SHELL",
              "script": "#!/bin/bash\necho 'Building...'\nmake build",
              "continueNoneZero": false,
              "additionalOptions": {
                "enable": true,
                "timeout": 30,
                "retryWhenFailed": true,
                "retryCount": 2
              }
            },
            {
              "@type": "marketBuild",
              "id": "e-2-1-2",
              "name": "上传制品",
              "atomCode": "uploadArtifact",
              "version": "1.*",
              "data": {
                "input": {
                  "filePath": "./build/output/*",
                  "destPath": "/artifacts/"
                }
              }
            }
          ]
        }
      ]
    },
    {
      "@type": "stage",
      "id": "stage-3",
      "name": "Deploy",
      "checkIn": {
        "manualTrigger": true,
        "reviewGroups": [
          {
            "name": "审核组",
            "reviewers": ["admin", "reviewer"]
          }
        ],
        "timeout": 24,
        "reviewDesc": "请确认是否部署到 ${env} 环境"
      },
      "containers": [
        {
          "@type": "normal",
          "id": "2",
          "name": "Deploy Job",
          "elements": [
            {
              "@type": "marketBuildLess",
              "id": "e-3-1-1",
              "name": "部署",
              "atomCode": "deploy",
              "version": "1.*"
            }
          ]
        }
      ]
    },
    {
      "@type": "stage",
      "id": "stage-4",
      "name": "Finally",
      "finally": true,
      "containers": [
        {
          "@type": "normal",
          "id": "3",
          "name": "Cleanup",
          "elements": [
            {
              "@type": "marketBuildLess",
              "id": "e-4-1-1",
              "name": "发送通知",
              "atomCode": "sendNotify",
              "version": "1.*"
            }
          ]
        }
      ]
    }
  ],
  "pipelineCreator": "admin"
}
```

---

## 总结

Model 是 BK-CI 的核心数据结构，理解 Model 的层次关系、字段含义和生命周期是开发流水线功能的基础。

**核心要点**:
1. **四层结构**: Model → Stage → Container → Element
2. **多态设计**: Container 和 Element 使用接口 + 实现类
3. **运行时字段**: 很多字段只在构建运行时有效，编排时不要设置
4. **版本兼容**: 通过 `transformCompatibility()` 处理历史数据
5. **JSON 存储**: Model 以 JSON 字符串存储在数据库中
6. **快照机制**: 每次构建会保存 Model 快照到构建记录表
7. **校验机制**: 通过 `ModelCheckPlugin` 进行完整性校验
8. **状态管理**: 通过 `BuildStatus` 枚举管理构建状态

掌握 Model 模型后，你将能够：
- 理解流水线的完整数据结构
- 开发流水线编排功能
- 扩展新的 Container 或 Element 类型
- 实现流水线导入导出
- 进行 Model 层面的校验和优化
- 理解构建执行引擎的工作原理
- 排查流水线执行问题

---

## 二十二、Model 持久化详解

### 22.1 数据库表结构

#### 22.1.1 流水线版本表 (T_PIPELINE_RESOURCE_VERSION)

存储流水线的**每个版本的完整 Model**：

```sql
CREATE TABLE `T_PIPELINE_RESOURCE_VERSION` (
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `VERSION` int(11) NOT NULL COMMENT '版本号',
  `VERSION_NAME` varchar(64) DEFAULT NULL COMMENT '版本名称',
  `MODEL` mediumtext COMMENT 'Model JSON字符串',
  `YAML` mediumtext COMMENT 'YAML 配置',
  `YAML_VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML 版本',
  `CREATOR` varchar(64) NOT NULL COMMENT '创建人',
  `UPDATER` varchar(64) DEFAULT NULL COMMENT '更新人',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `VERSION_NUM` int(11) DEFAULT NULL COMMENT '大版本号',
  `PIPELINE_VERSION` int(11) DEFAULT NULL COMMENT '编排版本',
  `TRIGGER_VERSION` int(11) DEFAULT NULL COMMENT '触发器版本',
  `SETTING_VERSION` int(11) DEFAULT NULL COMMENT '设置版本',
  `STATUS` varchar(16) DEFAULT NULL COMMENT '版本状态',
  `BRANCH_ACTION` varchar(32) DEFAULT NULL COMMENT '分支动作',
  `DESCRIPTION` text COMMENT '版本描述',
  `BASE_VERSION` int(11) DEFAULT NULL COMMENT '基础版本',
  `REFER_FLAG` bit(1) DEFAULT NULL COMMENT '引用标志',
  `RELEASE_TIME` datetime DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `VERSION`),
  KEY `idx_status` (`STATUS`),
  KEY `idx_release_time` (`RELEASE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线资源版本表';
```

**关键字段说明**：

| 字段 | 说明 |
|------|------|
| `VERSION` | 版本号，每次保存递增 |
| `VERSION_NAME` | 用户可见的版本名称 |
| `MODEL` | Model 的 JSON 序列化字符串 |
| `YAML` | PAC 模式下的 YAML 配置 |
| `STATUS` | 版本状态（RELEASED/COMMITTING/BRANCH/DELETE） |
| `BASE_VERSION` | 分支版本的基础版本号 |

#### 22.1.2 构建记录模型表 (T_PIPELINE_BUILD_RECORD_MODEL)

存储**构建运行时的 Model 状态**：

```sql
CREATE TABLE `T_PIPELINE_BUILD_RECORD_MODEL` (
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `RESOURCE_VERSION` int(11) NOT NULL COMMENT '资源版本',
  `BUILD_NUM` int(11) DEFAULT NULL COMMENT '构建号',
  `EXECUTE_COUNT` int(11) NOT NULL DEFAULT '1' COMMENT '执行次数',
  `START_USER` varchar(64) DEFAULT NULL COMMENT '启动用户',
  `START_TYPE` varchar(32) DEFAULT NULL COMMENT '启动类型',
  `MODEL_VAR` mediumtext COMMENT '模型变量（运行时状态）',
  `STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
  `START_TIME` datetime DEFAULT NULL COMMENT '开始时间',
  `END_TIME` datetime DEFAULT NULL COMMENT '结束时间',
  `ERROR_INFO` text COMMENT '错误信息',
  `CANCEL_USER` varchar(64) DEFAULT NULL COMMENT '取消用户',
  `TIMESTAMPS` text COMMENT '时间戳记录',
  PRIMARY KEY (`BUILD_ID`, `EXECUTE_COUNT`),
  KEY `idx_project_pipeline` (`PROJECT_ID`, `PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建记录模型表';
```

**关键字段说明**：

| 字段 | 说明 |
|------|------|
| `RESOURCE_VERSION` | 关联的流水线版本 |
| `EXECUTE_COUNT` | 执行次数（重试时递增） |
| `MODEL_VAR` | 运行时变量和状态的 JSON |
| `STATUS` | 当前构建状态 |
| `TIMESTAMPS` | 各阶段时间戳记录 |

### 22.2 DAO 层实现

#### 22.2.1 PipelineResourceVersionDao

```kotlin
@Repository
class PipelineResourceVersionDao {
    
    // 创建新版本
    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        versionName: String,
        model: Model,
        baseVersion: Int?,
        yamlStr: String?,
        yamlVersion: String?,
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?,
        versionStatus: VersionStatus?,
        branchAction: BranchVersionAction?,
        description: String?
    ): TPipelineResourceVersionRecord? {
        with(T_PIPELINE_RESOURCE_VERSION) {
            val modelStr = JsonUtil.toJson(model, formatted = false)
            val createTime = LocalDateTime.now()
            return dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(VERSION, version)
                .set(VERSION_NAME, versionName)
                .set(MODEL, modelStr)
                // ... 其他字段
                .onDuplicateKeyUpdate()
                .set(MODEL, modelStr)
                // ... 更新字段
                .returning()
                .fetchOne()
        }
    }
    
    // 获取指定版本的 Model 字符串
    fun getVersionModelString(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int?,
        includeDraft: Boolean? = null
    ): String? {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            val where = dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (version != null) {
                where.and(VERSION.eq(version))
            } else {
                if (includeDraft != true) where.and(
                    (STATUS.ne(VersionStatus.COMMITTING.name)
                        .and(STATUS.ne(VersionStatus.DELETE.name)))
                        .or(STATUS.isNull)
                )
                where.orderBy(VERSION.desc()).limit(1)
            }
            where.fetchOne()?.value1()
        }
    }
}
```

**文件位置**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/dao/PipelineResourceVersionDao.kt`

#### 22.2.2 BuildRecordModelDao

```kotlin
@Repository
class BuildRecordModelDao {
    
    // 创建构建记录
    fun createRecord(dslContext: DSLContext, record: BuildRecordModel) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            dslContext.insertInto(this)
                .set(BUILD_ID, record.buildId)
                .set(PROJECT_ID, record.projectId)
                .set(PIPELINE_ID, record.pipelineId)
                .set(RESOURCE_VERSION, record.resourceVersion)
                .set(BUILD_NUM, record.buildNum)
                .set(EXECUTE_COUNT, record.executeCount)
                .set(START_USER, record.startUser)
                .set(START_TYPE, record.startType)
                .set(MODEL_VAR, JsonUtil.toJson(record.modelVar, false))
                .set(STATUS, record.status)
                .set(ERROR_INFO, record.errorInfoList?.let { JsonUtil.toJson(it, false) })
                .set(CANCEL_USER, record.cancelUser)
                .set(TIMESTAMPS, JsonUtil.toJson(record.timestamps, false))
                .execute()
        }
    }
    
    // 更新构建记录
    fun updateRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus?,
        modelVar: Map<String, Any>,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        errorInfoList: List<ErrorInfo>?,
        cancelUser: String?,
        timestamps: Map<BuildTimestampType, BuildRecordTimeStamp>?
    ) {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            val update = dslContext.update(this)
                .set(MODEL_VAR, JsonUtil.toJson(modelVar, false))
            buildStatus?.let { update.set(STATUS, buildStatus.name) }
            cancelUser?.let { update.set(CANCEL_USER, cancelUser) }
            startTime?.let { update.set(START_TIME, startTime) }
            endTime?.let { update.set(END_TIME, endTime) }
            timestamps?.let {
                update.set(TIMESTAMPS, JsonUtil.toJson(timestamps, false))
            }
            errorInfoList?.let {
                update.set(ERROR_INFO, JsonUtil.toJson(errorInfoList, false))
            }
            update.where(
                BUILD_ID.eq(buildId)
                    .and(PROJECT_ID.eq(projectId))
                    .and(PIPELINE_ID.eq(pipelineId))
                    .and(EXECUTE_COUNT.eq(executeCount))
            ).execute()
        }
    }
    
    // 获取构建记录
    fun getRecord(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): BuildRecordModel? {
        with(TPipelineBuildRecordModel.T_PIPELINE_BUILD_RECORD_MODEL) {
            return dslContext.selectFrom(this)
                .where(
                    BUILD_ID.eq(buildId)
                        .and(PROJECT_ID.eq(projectId))
                        .and(PIPELINE_ID.eq(pipelineId))
                        .and(EXECUTE_COUNT.eq(executeCount))
                ).fetchAny(mapper)
        }
    }
}
```

**文件位置**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/dao/record/BuildRecordModelDao.kt`

---

## 二十三、Service 层业务逻辑

### 23.1 PipelineRepositoryService

PipelineRepositoryService 是 Model 持久化的**核心服务**：

```kotlin
@Service
class PipelineRepositoryService @Autowired constructor(
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val modelCheckPlugin: ModelCheckPlugin,
    // ... 其他依赖
) {
    
    /**
     * 获取流水线 Model
     */
    fun getModel(projectId: String, pipelineId: String): Model? {
        return pipelineResourceDao.getLatestVersionModelString(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.let { str2model(it, pipelineId) }
    }
    
    /**
     * 初始化并检查 Model 合法性
     */
    fun initModel(
        model: Model,
        projectId: String,
        pipelineId: String,
        userId: String,
        create: Boolean = true,
        versionStatus: VersionStatus? = VersionStatus.RELEASED,
        channelCode: ChannelCode,
        yamlInfo: PipelineYamlVo? = null,
        pipelineDialect: IPipelineDialect? = null
    ): List<PipelineModelTask> {
        // 1. 检查 Model 完整性
        val metaSize = modelCheckPlugin.checkModelIntegrity(
            model = model,
            projectId = projectId,
            userId = userId,
            oauthUser = getPipelineOauthUser(projectId, pipelineId),
            pipelineDialect = pipelineDialect,
            pipelineId = pipelineId
        )
        
        // 2. 去重 ID
        val distinctIdSet = HashSet<String>(metaSize, 1F)
        val jobIdDuplicateChecker = ModelIdDuplicateChecker()
        
        // 3. 初始化 ID
        val modelTasks = ArrayList<PipelineModelTask>(metaSize)
        val containerSeqId = AtomicInteger(0)
        
        // 4. 遍历初始化每个 Stage
        model.stages.forEachIndexed { index, s ->
            s.id = VMUtils.genStageId(index + 1)
            s.resetBuildOption(true)
            s.timeCost = null
            
            if (index == 0) {
                // 初始化触发容器
                initTriggerContainer(
                    stage = s,
                    containerSeqId = containerSeqId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    model = model,
                    userId = userId,
                    modelTasks = modelTasks,
                    channelCode = channelCode,
                    create = create,
                    distIds = distinctIdSet,
                    versionStatus = versionStatus,
                    yamlInfo = yamlInfo,
                    jobIdDuplicateChecker = jobIdDuplicateChecker
                )
            } else {
                // 初始化其他容器
                initOtherContainer(
                    stage = s,
                    projectId = projectId,
                    containerSeqId = containerSeqId,
                    userId = userId,
                    pipelineId = pipelineId,
                    model = model,
                    modelTasks = modelTasks,
                    channelCode = channelCode,
                    create = create,
                    distIds = distinctIdSet,
                    versionStatus = versionStatus,
                    yamlInfo = yamlInfo,
                    stageIndex = index,
                    jobIdDuplicateChecker = jobIdDuplicateChecker
                )
            }
        }
        
        // 5. 检查 Job ID 是否重复
        if (jobIdDuplicateChecker.duplicateIdSet.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_JOB_ID_DUPLICATE,
                params = arrayOf(jobIdDuplicateChecker.duplicateIdSet.joinToString(","))
            )
        }
        
        return modelTasks
    }
    
    /**
     * JSON 字符串转 Model
     */
    private fun str2model(modelString: String, pipelineId: String): Model {
        return try {
            JsonUtil.to(modelString, Model::class.java)
        } catch (e: Exception) {
            logger.error("[$pipelineId] str2model failed", e)
            throw e
        }
    }
}
```

**文件位置**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/PipelineRepositoryService.kt`

### 23.2 PipelineBuildDetailService

PipelineBuildDetailService 负责**构建详情的 Model 管理**：

```kotlin
@Service
class PipelineBuildDetailService @Autowired constructor(
    private val buildRecordModelDao: BuildRecordModelDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    // ... 其他依赖
) {
    
    /**
     * 更新构建 Model
     */
    fun updateModel(projectId: String, buildId: String, model: Model) {
        // 将 Model 状态更新到构建记录表
        val modelVar = extractModelVar(model)
        buildRecordModelDao.updateRecord(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = model.pipelineId,
            buildId = buildId,
            executeCount = model.executeCount,
            buildStatus = null,
            modelVar = modelVar,
            startTime = null,
            endTime = null,
            errorInfoList = null,
            cancelUser = null,
            timestamps = null
        )
    }
    
    /**
     * 从 Model 中提取变量
     */
    private fun extractModelVar(model: Model): Map<String, Any> {
        val modelVar = mutableMapOf<String, Any>()
        // 提取 Stage 状态
        model.stages.forEach { stage ->
            modelVar["stage_${stage.id}_status"] = stage.status ?: ""
            // 提取 Container 状态
            stage.containers.forEach { container ->
                modelVar["container_${container.id}_status"] = container.status ?: ""
                // 提取 Element 状态
                container.elements.forEach { element ->
                    modelVar["element_${element.id}_status"] = element.status ?: ""
                }
            }
        }
        return modelVar
    }
}
```

**文件位置**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/PipelineBuildDetailService.kt`

---

## 二十四、构建执行引擎中的 Model

### 24.1 BuildStartControl

BuildStartControl 负责**构建启动时的 Model 处理**：

```kotlin
@Service
class BuildStartControl @Autowired constructor(
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineContainerService: PipelineContainerService,
    private val taskRecordService: TaskRecordService,
    // ... 其他依赖
) {
    
    /**
     * 更新 Model（构建启动时）
     */
    private fun updateModel(model: Model, buildInfo: BuildInfo, taskId: String, executeCount: Int) {
        val now = LocalDateTime.now()
        val stage = model.stages[0]
        val container = stage.containers[0]
        
        // 1. 更新触发器 Element 状态
        run lit@{
            container.name = ContainerUtils.getClearedQueueContainerName(container.name)
            container.elements.forEach {
                if (it.id == taskId) {
                    // 更新 Container 状态
                    pipelineContainerService.updateContainerStatus(
                        projectId = buildInfo.projectId,
                        buildId = buildInfo.buildId,
                        stageId = stage.id!!,
                        containerId = container.id!!,
                        startTime = now,
                        endTime = now,
                        buildStatus = BuildStatus.SUCCEED
                    )
                    // 更新 Task 状态
                    taskRecordService.updateTaskStatus(
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        buildId = buildInfo.buildId,
                        taskId = taskId,
                        buildStatus = BuildStatus.SUCCEED,
                        executeCount = executeCount,
                        operation = "updateTriggerElement#$taskId"
                    )
                    it.status = BuildStatus.SUCCEED.name
                    return@lit
                }
            }
        }
        
        // 2. 更新 Stage 状态
        pipelineStageService.updateStageStatus(
            projectId = buildInfo.projectId,
            buildId = buildInfo.buildId,
            stageId = stage.id!!,
            buildStatus = BuildStatus.SUCCEED,
            checkIn = stage.checkIn,
            checkOut = stage.checkOut
        )
        
        // 3. 更新 Model 记录
        pipelineRecordService.updateModelRecord(
            projectId = buildInfo.projectId,
            pipelineId = buildInfo.pipelineId,
            buildId = buildInfo.buildId,
            executeCount = executeCount,
            buildStatus = null,
            modelVar = mutableMapOf(),
            timestamps = mapOf(
                BuildTimestampType.BUILD_CONCURRENCY_QUEUE to
                    BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
            ),
            startTime = LocalDateTime.now(),
            endTime = null
        )
        
        // 4. 计算耗时并更新
        val nowMills = now.timestampmilli()
        val stageElapsed = max(0, nowMills - buildInfo.queueTime)
        stage.elapsed = stageElapsed
        stage.status = BuildStatus.SUCCEED.name
        
        // 5. 更新 Container 运行时状态
        container.status = BuildStatus.SUCCEED.name
        container.startEpoch = nowMills
        container.systemElapsed = stage.elapsed
        container.elementElapsed = 0
        container.executeCount = executeCount
        container.startVMStatus = BuildStatus.SUCCEED.name
        
        // 6. 持久化 Model 更新
        buildDetailService.updateModel(
            projectId = buildInfo.projectId,
            buildId = buildInfo.buildId,
            model = model
        )
    }
}
```

**文件位置**: `src/backend/ci/core/process/biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/BuildStartControl.kt`

### 24.2 Model 状态更新流程

```
构建启动
    ↓
从 T_PIPELINE_RESOURCE_VERSION 获取 Model
    ↓
创建 T_PIPELINE_BUILD_RECORD_MODEL 记录
    ↓
初始化所有 Stage/Container/Element 状态为 QUEUE
    ↓
执行第一个 Stage
    ↓
更新 Stage 状态为 RUNNING
    ↓
调度 Container
    ↓
更新 Container 状态为 PREPARE_ENV → RUNNING
    ↓
执行 Element
    ↓
更新 Element 状态为 RUNNING → SUCCEED/FAILED
    ↓
Container 完成，更新状态
    ↓
Stage 完成，更新状态
    ↓
所有 Stage 完成
    ↓
更新 Model 最终状态
    ↓
构建结束
```

---

## 二十五、Model 版本管理

### 25.1 版本状态 (VersionStatus)

```kotlin
enum class VersionStatus {
    RELEASED,      // 已发布（正式版本）
    COMMITTING,    // 提交中（草稿）
    BRANCH,        // 分支版本
    DELETE         // 已删除
}
```

### 25.2 版本号规则

BK-CI 使用**多维版本号**系统：

```kotlin
data class PipelineVersionSimple(
    val version: Int,           // 内部版本号（自增）
    val versionName: String,    // 用户可见版本名
    val versionNum: Int?,       // 大版本号
    val pipelineVersion: Int?,  // 编排版本
    val triggerVersion: Int?,   // 触发器版本
    val settingVersion: Int?    // 设置版本
)
```

**版本号示例**：
- `version = 15`：内部版本号
- `versionName = "V1.5"`：用户可见版本
- `versionNum = 1`：大版本
- `pipelineVersion = 5`：编排修改次数
- `triggerVersion = 2`：触发器修改次数
- `settingVersion = 3`：设置修改次数

### 25.3 分支版本 (BranchVersionAction)

```kotlin
enum class BranchVersionAction {
    ACTIVE,     // 活跃分支
    INACTIVE,   // 非活跃分支
    MERGED      // 已合并
}
```

**分支版本用途**：
- 支持多人协作开发
- 支持 PAC (Pipeline as Code) 模式
- 支持版本回滚

---

## 二十六、Model 与 YAML 转换

### 26.1 Model 转 YAML

```kotlin
// Model 转换为 YAML 格式
fun Model.toYaml(): String {
    val preModel = PreScriptBuildYaml(
        version = "v2.0",
        name = this.name,
        stages = this.stages.mapIndexed { index, stage ->
            if (index == 0) {
                // 触发器 Stage 转换为 on 配置
                null
            } else {
                stage.toPreStage()
            }
        }.filterNotNull()
    )
    return YamlUtil.toYaml(preModel)
}

// Stage 转换
fun Stage.toPreStage(): PreStage {
    return PreStage(
        name = this.name,
        jobs = this.containers.map { it.toPreJob() }
    )
}

// Container 转换
fun Container.toPreJob(): PreJob {
    return PreJob(
        name = this.name,
        runsOn = when (this) {
            is VMBuildContainer -> this.dispatchType?.toRunsOn()
            else -> null
        },
        steps = this.elements.map { it.toPreStep() }
    )
}

// Element 转换
fun Element.toPreStep(): PreStep {
    return PreStep(
        name = this.name,
        id = this.stepId,
        uses = "${this.getAtomCode()}@${this.version}",
        with = this.genTaskParams()
    )
}
```

### 26.2 YAML 转 Model

```kotlin
// YAML 转换为 Model
fun PreScriptBuildYaml.toModel(): Model {
    return Model(
        name = this.name ?: "Pipeline",
        desc = "",
        stages = listOf(
            // 触发器 Stage
            Stage(
                id = "stage-1",
                containers = listOf(
                    TriggerContainer(
                        id = "0",
                        name = "trigger",
                        elements = this.on?.toTriggerElements() ?: listOf(
                            ManualTriggerElement()
                        )
                    )
                )
            )
        ) + this.stages.mapIndexed { index, preStage ->
            preStage.toStage(index + 2)
        }
    )
}
```

**文件位置**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/service/pipeline/PipelineTransferYamlService.kt`

---

## 二十七、Model 扩展指南

### 27.1 新增 Element 类型

**步骤**：

1. **定义 Element 类**：
```kotlin
// 在 common-pipeline 模块
data class MyCustomElement(
    override val name: String = "我的自定义插件",
    override var id: String? = null,
    override var status: String? = null,
    override var stepId: String? = null,
    
    // 自定义字段
    val myParam1: String,
    val myParam2: Int = 0,
    
    override var additionalOptions: ElementAdditionalOptions? = null
) : Element(name, id, status, additionalOptions = additionalOptions) {
    
    companion object {
        const val classType = "myCustomElement"
    }
    
    override fun getClassType() = classType
    
    override fun getAtomCode() = "myCustomAtom"
}
```

2. **注册到 JsonSubTypes**：
```kotlin
// 在 Element.kt 的 @JsonSubTypes 中添加
@JsonSubTypes.Type(value = MyCustomElement::class, name = MyCustomElement.classType)
```

3. **实现插件执行逻辑**（在 Worker 端）

### 27.2 新增 Container 类型

**步骤**：

1. **定义 Container 类**：
```kotlin
data class MyCustomContainer(
    override var id: String? = null,
    override var name: String = "",
    override var elements: List<Element> = listOf(),
    
    // 自定义字段
    val customConfig: MyConfig?,
    
    // ... 其他 Container 接口字段
) : Container {
    
    companion object {
        const val classType = "myCustom"
    }
    
    override fun getClassType() = classType
    
    // 实现其他接口方法
}
```

2. **注册到 JsonSubTypes**：
```kotlin
// 在 Container.kt 的 @JsonSubTypes 中添加
@JsonSubTypes.Type(value = MyCustomContainer::class, name = MyCustomContainer.classType)
```

3. **实现调度逻辑**（在 Dispatch 服务中）

---

## 二十八、调试与排查

### 28.1 常用调试方法

**打印 Model JSON**：
```kotlin
val modelJson = JsonUtil.toJson(model, formatted = true)
logger.info("Model: $modelJson")
```

**检查 Model 结构**：
```kotlin
fun debugModel(model: Model) {
    println("Pipeline: ${model.name}")
    model.stages.forEachIndexed { sIndex, stage ->
        println("  Stage[$sIndex]: ${stage.id} - ${stage.name} - ${stage.status}")
        stage.containers.forEachIndexed { cIndex, container ->
            println("    Container[$cIndex]: ${container.id} - ${container.name} - ${container.status}")
            container.elements.forEachIndexed { eIndex, element ->
                println("      Element[$eIndex]: ${element.id} - ${element.name} - ${element.status}")
            }
        }
    }
}
```

### 28.2 常见问题排查

**问题1：Model 反序列化失败**
```
原因：Element 或 Container 的 @type 字段不匹配
排查：检查 JSON 中的 @type 值是否在 @JsonSubTypes 中注册
```

**问题2：构建状态不更新**
```
原因：Model 更新后未持久化
排查：检查 buildDetailService.updateModel() 是否被调用
```

**问题3：版本号不递增**
```
原因：版本状态不是 RELEASED
排查：检查 versionStatus 参数
```

---

## 二十九、性能优化建议

### 29.1 Model 序列化优化

```kotlin
// 使用非格式化 JSON（减少空格和换行）
val modelJson = JsonUtil.toJson(model, formatted = false)

// 使用压缩存储（对于大型 Model）
val compressedModel = GZIPUtils.compress(modelJson)
```

### 29.2 查询优化

```kotlin
// 只获取需要的字段
fun getModelStatus(buildId: String): String? {
    return buildRecordModelDao.getStatus(dslContext, buildId)
}

// 批量查询
fun batchGetModels(buildIds: List<String>): Map<String, Model> {
    return buildRecordModelDao.batchGet(dslContext, buildIds)
}
```

### 29.3 缓存策略

```kotlin
// 使用 Redis 缓存热点 Model
@Cacheable(cacheNames = ["model"], key = "#pipelineId + ':' + #version")
fun getModel(pipelineId: String, version: Int): Model? {
    return pipelineResourceVersionDao.getVersionModel(dslContext, pipelineId, version)
}
```

---

## 三十、附录：完整类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                   Model                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ + name: String                                                              │
│ + desc: String?                                                             │
│ + stages: List<Stage>                                                       │
│ + pipelineCreator: String?                                                  │
│ + latestVersion: Int                                                        │
│ + timeCost: BuildRecordTimeCost?                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ + getTriggerContainer(): TriggerContainer                                   │
│ + taskCount(): Int                                                          │
│ + removeElements(types: Set<String>): Model                                │
│ + defaultModel(name: String, userId: String?): Model                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ 1:N
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                   Stage                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ + id: String?                                                               │
│ + name: String?                                                             │
│ + containers: List<Container>                                               │
│ + stageControlOption: StageControlOption?                                   │
│ + checkIn: StagePauseCheck?                                                 │
│ + checkOut: StagePauseCheck?                                                │
│ + finally: Boolean                                                          │
│ + status: String?                                                           │
│ + timeCost: BuildRecordTimeCost?                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ + resetBuildOption(init: Boolean?)                                          │
│ + getContainer(vmSeqId: String): Container?                                │
│ + stageEnabled(): Boolean                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ 1:N
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              <<interface>>                                   │
│                                Container                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ + id: String?                                                               │
│ + name: String                                                              │
│ + elements: List<Element>                                                   │
│ + status: String?                                                           │
│ + executeCount: Int?                                                        │
│ + containerHashId: String?                                                  │
│ + jobId: String?                                                            │
│ + timeCost: BuildRecordTimeCost?                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ + getClassType(): String                                                    │
│ + containerEnabled(): Boolean                                               │
│ + resetBuildOption(executeCount: Int)                                       │
│ + transformCompatibility()                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
              ▲                       ▲                       ▲
              │                       │                       │
    ┌─────────┴─────────┐   ┌────────┴────────┐   ┌─────────┴─────────┐
    │ TriggerContainer  │   │ VMBuildContainer│   │ NormalContainer   │
    ├───────────────────┤   ├─────────────────┤   ├───────────────────┤
    │ + params: List    │   │ + baseOS        │   │ + jobControlOption│
    │ + buildNo         │   │ + dispatchType  │   │ + mutexGroup      │
    │ + templateParams  │   │ + jobControlOpt │   │ + matrixControl   │
    └───────────────────┘   │ + mutexGroup    │   └───────────────────┘
                            │ + matrixControl │
                            │ + groupContainers│
                            └─────────────────┘
                                      │
                                      │ 1:N
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              <<abstract>>                                    │
│                                Element                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ + name: String                                                              │
│ + id: String?                                                               │
│ + status: String?                                                           │
│ + version: String                                                           │
│ + stepId: String?                                                           │
│ + additionalOptions: ElementAdditionalOptions?                              │
│ + timeCost: BuildRecordTimeCost?                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ + getClassType(): String                                                    │
│ + getAtomCode(): String                                                     │
│ + elementEnabled(): Boolean                                                 │
│ + transformCompatibility()                                                  │
│ + initStatus(rerun: Boolean): BuildStatus                                  │
└─────────────────────────────────────────────────────────────────────────────┘
              ▲                       ▲                       ▲
              │                       │                       │
    ┌─────────┴─────────┐   ┌────────┴────────┐   ┌─────────┴─────────┐
    │ManualTriggerElement│  │LinuxScriptElement│  │MarketBuildAtomElement│
    ├───────────────────┤   ├─────────────────┤   ├─────────────────────┤
    │ + canElementSkip  │   │ + scriptType    │   │ + atomCode          │
    │ + useLatestParams │   │ + script        │   │ + data              │
    │ + buildMsg        │   │ + continueNone  │   │                     │
    └───────────────────┘   └─────────────────┘   └─────────────────────┘
```

---

**文档版本**: 2.1  
**最后更新**: 2024-12  
**维护者**: BK-CI Team
