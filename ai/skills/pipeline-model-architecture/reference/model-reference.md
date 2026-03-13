# BK-CI 流水线模型完整参考

本文件包含 Model 各层级的完整数据结构定义、辅助类型、枚举值和业务逻辑。

---

## Model 顶层结构

```kotlin
data class Model(
    var name: String,                           // 流水线名称
    var desc: String?,                          // 流水线描述
    val stages: List<Stage>,                    // 阶段集合（核心）
    var labels: List<String> = emptyList(),     // 标签（已废弃）
    val instanceFromTemplate: Boolean? = null,  // 是否从模板实例化
    var srcTemplateId: String? = null,          // 源模板ID
    var templateId: String? = null,             // 当前模板ID
    var template: TemplateInstanceDescriptor?,  // 模板实例描述符
    var overrideTemplateField: TemplateInstanceField?,
    var pipelineCreator: String? = null,
    var latestVersion: Int = 0,
    var tips: String? = null,
    var events: Map<String, PipelineCallbackEvent>?,
    var staticViews: List<String> = emptyList(),
    var timeCost: BuildRecordTimeCost? = null,
    val resources: Resources? = null
)
```

**核心方法**:
- `getTriggerContainer()` — 返回 `stages[0].containers[0] as TriggerContainer`
- `taskCount(skipTaskClassType)` — 遍历统计 Element 数量
- `removeElements(elementClassTypes)` — 过滤指定 classType 的 Element，返回新 Model
- `Model.defaultModel(pipelineName, userId)` — 创建最小化模型（仅含手动触发器）

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/Model.kt`

---

## Stage 数据结构

```kotlin
data class Stage(
    val containers: List<Container> = listOf(),
    var id: String?,                             // 系统生成 stage-{seq}
    var name: String? = "",
    var stageIdForUser: String? = null,          // 用户可编辑ID
    var stageControlOption: StageControlOption?,
    var checkIn: StagePauseCheck? = null,        // 准入审核
    var checkOut: StagePauseCheck? = null,       // 准出审核
    val finally: Boolean = false,                // FinallyStage标识
    val fastKill: Boolean? = false,
    var status: String? = null,
    var executeCount: Int? = null,
    var canRetry: Boolean? = null,
    var timeCost: BuildRecordTimeCost? = null,
    val customBuildEnv: Map<String, String>?,
    var tag: List<String>? = null,
    var template: TemplateDescriptor? = null
)
```

**FinallyStage 规则**: 每个 Model 最多一个，必须在最后位置，无论成功失败都执行，不可重试。

**准入准出流程**: `Stage开始 → checkIn审核 → 执行Containers → checkOut审核 → Stage结束`

**核心方法**:
- `resetBuildOption(init)` — 重置运行时状态，FinallyStage 自动设 `canRetry = false`
- `getContainer(vmSeqId)` — 按 ID 查找容器
- `stageEnabled()` — 检查 `stageControlOption?.enable`

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/container/Stage.kt`

---

## Container 接口与实现

### Container 接口

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TriggerContainer::class, name = TriggerContainer.classType),
    JsonSubTypes.Type(value = NormalContainer::class, name = NormalContainer.classType),
    JsonSubTypes.Type(value = VMBuildContainer::class, name = VMBuildContainer.classType),
    JsonSubTypes.Type(value = JobTemplateContainer::class, name = JobTemplateContainer.classType)
)
interface Container {
    var id: String?
    var name: String
    var elements: List<Element>
    var status: String?
    var startVMStatus: String?
    var executeCount: Int?
    var canRetry: Boolean?
    var containerId: String?
    var containerHashId: String?       // 全局唯一HashID
    var jobId: String?                 // 用户自定义ID
    var timeCost: BuildRecordTimeCost?
    var containPostTaskFlag: Boolean?
    val matrixGroupFlag: Boolean?

    fun getClassType(): String
    fun getContainerById(vmSeqId: String): Container?
    fun containerEnabled(): Boolean
    fun resetBuildOption(executeCount: Int)
    fun transformCompatibility()
    fun genTaskParams(): MutableMap<String, Any>
    fun copyElements(elements: List<Element>): Container
    fun retryFreshMatrixOption()
    fun fetchGroupContainers(): List<Container>?
    fun fetchMatrixContext(): Map<String, String>?
}
```

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/container/Container.kt`

### TriggerContainer (classType = "trigger")

- 固定位置: `stages[0].containers[0]`
- 包含全局参数 `params: List<BuildFormProperty>` 和模板参数 `templateParams`
- 包含构建版本号规则 `buildNo: BuildNo?`
- 不可删除，唯一

```kotlin
data class BuildFormProperty(
    var id: String,
    var name: String?,
    var required: Boolean,
    var type: BuildFormPropertyType,     // STRING, BOOLEAN, ENUM, GIT_REF, PASSWORD 等
    var defaultValue: Any,
    var value: Any? = null,
    var options: List<BuildFormValue>?,
    var desc: String?,
    var asInstanceInput: Boolean? = null
)
```

### VMBuildContainer (classType = "vmBuild")

关键字段:
- `baseOS: VMBaseOS` — LINUX / WINDOWS / MACOS
- `dispatchType: DispatchType?` — 构建机调度类型
- `jobControlOption: JobControlOption?` — Job 流程控制
- `mutexGroup: MutexGroup?` — 互斥组
- `matrixControlOption: MatrixControlOption?` — 构建矩阵配置
- `groupContainers` — 矩阵分裂后的子容器

### NormalContainer (classType = "normal")

无编译环境容器，用于人工审核、质量红线、API 调用等无需构建机的任务。

---

## Element 抽象类与子类型

### Element 基类

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
abstract class Element(
    open val name: String,
    open var id: String? = null,
    open var status: String? = null,
    open var executeCount: Int = 1,
    open var canRetry: Boolean? = null,
    open var version: String = "1.*",
    open var additionalOptions: ElementAdditionalOptions? = null,
    open var stepId: String? = null,       // 用户自定义ID，用于 steps.myStep.status
    open var customEnv: List<NameAndValue>? = null,
    open var errorType: String? = null,
    open var errorCode: Int? = null,
    open var errorMsg: String? = null,
    open var timeCost: BuildRecordTimeCost? = null
)
```

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/Element.kt`

### ElementAdditionalOptions（插件级流程控制）

```kotlin
data class ElementAdditionalOptions(
    var enable: Boolean = true,
    var continueWhenFailed: Boolean = false,
    val manualSkip: Boolean? = null,
    var retryWhenFailed: Boolean = false,
    var retryCount: Int = 0,
    val manualRetry: Boolean = true,
    var timeout: Long? = 100,
    var timeoutVar: String? = null,
    var runCondition: RunCondition? = null,
    val customVariables: List<NameAndValue>?,
    var customCondition: String? = "",
    var pauseBeforeExec: Boolean? = false,
    var subscriptionPauseUser: String? = "",
    var elementPostInfo: ElementPostInfo? = null
)
```

**RunCondition 值**: `PRE_TASK_SUCCESS` | `PRE_TASK_FAILED_BUT_CANCEL` | `PRE_TASK_FAILED_EVEN_CANCEL` | `PRE_TASK_FAILED_ONLY` | `CUSTOM_VARIABLE_MATCH` | `CUSTOM_VARIABLE_MATCH_NOT_RUN` | `PARENT_TASK_CANCELED_OR_TIMEOUT` | `PARENT_TASK_FINISH`

### 常见 Element 子类型

**触发器**: ManualTriggerElement (`manualTrigger`) | TimerTriggerElement (`timerTrigger`) | CodeGitWebHookTriggerElement | CodeGitlabWebHookTriggerElement | RemoteTriggerElement

**代码拉取**: CodeGitElement | CodeGitlabElement | CodeSvnElement | GithubElement

**脚本执行**: LinuxScriptElement (`linuxScript`) | WindowsScriptElement (`windowsScript`)

**市场插件**: MarketBuildAtomElement (`marketBuild`) | MarketBuildLessAtomElement (`marketBuildLess`)

**其他**: ManualReviewUserTaskElement | SubPipelineCallElement | QualityGateInElement | QualityGateOutElement

---

## 辅助数据结构

### DispatchType（构建机调度）

| @type 值 | 类 | 场景 |
|----------|---|------|
| `DOCKER` | DockerDispatchType | 公共构建机 / Docker 镜像 |
| `KUBERNETES` | KubernetesDispatchType | K8s 集群构建 |
| `THIRD_PARTY_AGENT_ID` | ThirdPartyAgentIDDispatchType | 指定第三方构建机 |
| `THIRD_PARTY_AGENT_ENV` | ThirdPartyAgentEnvDispatchType | 第三方环境池 |
| `THIRD_PARTY_DEVCLOUD` | ThirdPartyDevCloudDispatchType | 云开发机 |

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/type/DispatchType.kt`

### JobControlOption

```kotlin
data class JobControlOption(
    val enable: Boolean = true,
    val prepareTimeout: Int? = null,
    var timeout: Int? = 900,                // 分钟
    var timeoutVar: String? = null,
    val runCondition: JobRunCondition = JobRunCondition.STAGE_RUNNING,
    val customVariables: List<NameAndValue>?,
    val customCondition: String? = null,
    val dependOnType: DependOnType? = null,
    var dependOnId: List<String>? = null,
    val continueWhenFailed: Boolean? = false,
    val singleNodeConcurrency: Int? = null,
    val allNodeConcurrency: Int? = null
)
```

**JobRunCondition 值**: `STAGE_RUNNING` | `CUSTOM_VARIABLE_MATCH` | `CUSTOM_VARIABLE_MATCH_NOT_RUN` | `CUSTOM_CONDITION_MATCH` | `PREVIOUS_STAGE_SUCCESS` | `PREVIOUS_STAGE_FAILED` | `PREVIOUS_STAGE_CANCEL`

### MutexGroup（互斥组）

```kotlin
data class MutexGroup(
    val enable: Boolean,
    val mutexGroupName: String? = "",
    val queueEnable: Boolean,
    var timeout: Int = 0,                   // 0 = 不等待直接失败
    var timeoutVar: String? = null,
    val queue: Int = 0,
    var runtimeMutexGroup: String? = null,
    var linkTip: String? = null
)
```

Redis Key 格式: `lock:container:mutex:{projectId}:{mutexGroupName}:lock` / `...:queue`

### MatrixControlOption（构建矩阵）

```kotlin
data class MatrixControlOption(
    val strategyStr: String? = null,        // JSON/YAML 格式策略
    val includeCaseStr: String? = null,
    val excludeCaseStr: String? = null,
    val fastKill: Boolean? = false,
    var maxConcurrency: Int? = 5,
    var customDispatchInfo: DispatchInfo?,
    var totalCount: Int? = null,            // 运行时
    var finishCount: Int? = null            // 运行时
)
// MATRIX_CASE_MAX_COUNT = 256
```

展开流程: 解析 strategyStr 笛卡尔积 → 添加 include → 移除 exclude → 生成子容器

### StagePauseCheck（准入准出）

```kotlin
data class StagePauseCheck(
    var manualTrigger: Boolean? = false,
    var status: String? = null,
    var reviewDesc: String? = null,
    var reviewGroups: MutableList<StageReviewGroup>?,
    var reviewParams: List<ManualReviewParam>?,
    var timeout: Int? = 24,                 // 小时
    var ruleIds: List<String>? = null,      // 质量红线规则
    var notifyType: MutableList<String>?,
    var notifyGroup: MutableList<String>?
)
```

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/StagePauseCheck.kt`

### BuildNo（构建版本号）

```kotlin
data class BuildNo(
    var buildNo: Int,
    val buildNoType: BuildNoType,   // CONSISTENT | SUCCESS_BUILD_INCREMENT | EVERY_BUILD_INCREMENT
    var required: Boolean? = false,
    var currentBuildNo: Int? = null
)
```

### BuildRecordTimeCost

```kotlin
data class BuildRecordTimeCost(
    var systemCost: Long = 0,
    var executeCost: Long = 0,
    var waitCost: Long = 0,      // 包含 queueCost
    var queueCost: Long = 0,
    var totalCost: Long = 0      // = systemCost + executeCost + waitCost
)
```

适用于 Model / Stage / Container / Element 各层级。

### ElementPostInfo

```kotlin
data class ElementPostInfo(
    val postEntryParam: String,
    val postCondition: String,         // always | success | failure
    var parentElementId: String,
    val parentElementName: String,
    val parentElementJobIndex: Int
)
```

---

## BuildStatus 枚举

### 成功态
`SUCCEED` | `SKIP` | `REVIEW_PROCESSED` | `QUALITY_CHECK_PASS` | `STAGE_SUCCESS`

### 失败态
`FAILED` | `CANCELED` | `TERMINATE` | `REVIEW_ABORT` | `HEARTBEAT_TIMEOUT` | `QUALITY_CHECK_FAIL` | `QUEUE_TIMEOUT` | `EXEC_TIMEOUT` | `QUOTA_FAILED`

### 运行态
`RUNNING` | `REVIEWING` | `PREPARE_ENV` | `LOOP_WAITING` | `CALL_WAITING` | `PAUSE` | `DEPENDENT_WAITING` | `QUALITY_CHECK_WAIT` | `TRIGGER_REVIEWING`

### 初始态
`QUEUE` | `QUEUE_CACHE` | `RETRY`

### 判断方法
- `isFinish()` = `isFailure() || isSuccess() || isCancel()`
- `isRunning()` = RUNNING / LOOP_WAITING 等
- `isReadyToRun()` = QUEUE / QUEUE_CACHE / RETRY

**状态流转**: `QUEUE → QUEUE_CACHE → PREPARE_ENV → RUNNING → SUCCEED/FAILED/CANCELED`

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/BuildStatus.kt`

---

## BuildFormPropertyType（参数类型）

| 类型 | 前端组件 | 说明 |
|-----|---------|------|
| `STRING` | 单行输入框 | 普通字符串 |
| `TEXTAREA` | 多行输入框 | 长文本 |
| `ENUM` | 下拉选择框 | 配合 options |
| `BOOLEAN` | 开关/复选框 | true/false |
| `GIT_REF` | 分支选择器 | 代码库分支列表 |
| `CODE_LIB` | 代码库选择器 | 项目代码库 |
| `SUB_PIPELINE` | 流水线选择器 | 项目流水线 |
| `PASSWORD` | 密码输入框 | 加密存储 |
| `DATE` / `LONG` / `SVN_TAG` / `REPO_REF` / `MULTIPLE` / `CONTAINER_TYPE` / `ARTIFACTORY` / `CUSTOM_FILE` / `TEMPORARY` | 各类特殊选择器 | |

**文件**: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/BuildFormPropertyType.kt`

---

## 运行条件枚举

### StageRunCondition
`AFTER_LAST_FINISHED` | `CUSTOM_VARIABLE_MATCH` | `CUSTOM_VARIABLE_MATCH_NOT_RUN` | `CUSTOM_CONDITION_MATCH`

### VMBaseOS
`LINUX` | `WINDOWS` | `MACOS` | `ALL`（无编译环境）

---

## 版本管理

### VersionStatus
`RELEASED` | `COMMITTING`（草稿） | `BRANCH` | `DELETE`

### 多维版本号

```kotlin
data class PipelineVersionSimple(
    val version: Int,            // 内部自增版本号
    val versionName: String,     // 用户可见 "V1.5"
    val versionNum: Int?,        // 大版本号
    val pipelineVersion: Int?,   // 编排修改次数
    val triggerVersion: Int?,    // 触发器修改次数
    val settingVersion: Int?     // 设置修改次数
)
```

### BranchVersionAction
`ACTIVE` | `INACTIVE` | `MERGED` — 支持 PAC 模式多人协作

---

## Model 校验机制

### ModelCheckPlugin 接口

```kotlin
interface ModelCheckPlugin {
    fun checkModelIntegrity(model: Model, projectId: String?, userId: String, ...): Int
    fun checkSettingIntegrity(setting: PipelineSetting, projectId: String?)
    fun clearUpModel(model: Model)
    fun beforeDeleteElementInExistsModel(existModel: Model, sourceModel: Model?, param: BeforeDeleteParam)
    fun checkElementTimeoutVar(container: Container, element: Element, contextMap: Map<String, String>)
    fun checkMutexGroup(container: Container, contextMap: Map<String, String>)
    fun checkJobCondition(container: Container, finallyStage: Boolean, contextMap: Map<String, String>)
}
```

### 默认校验项

| 项目 | 默认限制 |
|------|---------|
| 流水线名称长度 | 最大 64 字符 |
| 流水线描述长度 | 最大 100 字符 |
| Model JSON 大小 | 最大 4MB |
| Stage 数量 | 最大 20 |
| 每 Stage Job 数量 | 最大 20 |
| 每 Job Element 数量 | 最大 50 |
| FinallyStage | 必须在末尾，最多 1 个 |
| TriggerContainer | 必须在 stages[0].containers[0] |

**文件**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/extend/DefaultModelCheckPlugin.kt`

---

## Element 目录结构

```
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/element/
├── Element.kt / ElementAdditionalOptions.kt / ElementPostInfo.kt / EmptyElement.kt
├── SubPipelineCallElement.kt / StepTemplateElement.kt
├── agent/           — CodeGitElement, LinuxScriptElement, WindowsScriptElement, ManualReviewUserTaskElement 等
├── trigger/         — ManualTriggerElement, TimerTriggerElement, *WebHookTriggerElement 等
├── market/          — MarketBuildAtomElement, MarketBuildLessAtomElement 等
├── quality/         — QualityGateInElement, QualityGateOutElement
├── matrix/          — MatrixStatusElement
└── atom/            — ManualReviewParam, BeforeDeleteParam 等
```
