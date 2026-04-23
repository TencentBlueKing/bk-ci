# API 与组件详解

本文档包含 YAML 流水线转换系统的完整接口定义和数据模型。

---

## TransferMapper（转换映射器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/TransferMapper.kt`

核心序列化/反序列化引擎，处理 YAML 与对象之间的转换。

### 关键方法

```kotlin
object TransferMapper {
    fun <T> to(str: String): T                    // YAML 字符串转对象
    fun toYaml(bean: Any): String                  // 对象转 YAML 字符串
    fun <T> anyTo(any: Any?): T                    // 任意对象转换
    fun formatYaml(yaml: String): String           // 格式化 YAML
    fun mergeYaml(old: String, new: String): String // 合并 YAML（保留注释和锚点）
    fun getYamlLevelOneIndex(yaml: String): Map<String, TransferMark>  // 第一层级坐标
    fun indexYaml(yaml: String, line: Int, column: Int): NodeIndex?    // 节点索引
    fun markYaml(index: NodeIndex, yaml: String): TransferMark?       // 标记节点位置
    fun getYamlFactory(): Yaml
    fun getObjectMapper(): ObjectMapper
}
```

### CustomStringQuotingChecker

处理 YAML 特殊关键字的引号规则：

```kotlin
class CustomStringQuotingChecker : StringQuotingChecker() {
    override fun needToQuoteName(name: String): Boolean {
        if (name == "on") return false  // on 关键字不加引号
        return reservedKeyword(name) || looksLikeYAMLNumber(name)
    }

    private fun looksLikeHexNumber(value: String): Boolean {
        if (value.length < 3) return false
        return value.startsWith("0x", ignoreCase = true)  // 0x 开头需要加引号
    }
}
```

### mergeYaml 实现

使用 Myers Diff 算法智能合并两个 YAML，保留注释和锚点：

```kotlin
fun mergeYaml(old: String, new: String): String {
    if (old.isBlank()) return new

    val oldE = getYamlFactory().parse(old.reader()).toList()
    val newE = getYamlFactory().parse(new.reader()).toMutableList()

    val patch = DiffUtils.diff(oldE, newE, MeyersDiffWithLinearSpace.factory().create())

    for (delta in patch.deltas) {
        when (delta.type) {
            DeltaType.DELETE -> {
                val sourceComment = checkCommentEvent(delta.source.lines)
                if (sourceComment.isNotEmpty()) {
                    newE.addAll(delta.target.position, sourceComment)
                }
            }
            DeltaType.INSERT -> {
                anchorChecker[delta.source.position]?.let { checker -> /* 恢复锚点 */ }
            }
        }
    }

    val newNode = eventsComposer(newE).singleNode
    replaceAnchor(newNode, anchorNodes)
    return getYamlFactory().serialize(newNode)
}
```

---

## ModelTransfer（Model 转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/ModelTransfer.kt`

### 类定义

```kotlin
@Component
class ModelTransfer @Autowired constructor(
    val client: Client,
    val modelStage: StageTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer,
    val transferCache: TransferCacheService
) {
    fun yaml2Model(yamlInput: YamlTransferInput): Model
    fun yaml2Setting(yamlInput: YamlTransferInput): PipelineSetting
    fun yaml2Labels(yamlInput: YamlTransferInput): List<String>
    fun model2Yaml(input: ModelTransferInput): PreTemplateScriptBuildYamlParser
}
```

### yaml2Model 流程

```kotlin
fun yaml2Model(yamlInput: YamlTransferInput): Model {
    // 1. 前置切面处理
    yamlInput.aspectWrapper.setYaml4Yaml(yamlInput.yaml, BEFORE)

    // 2. 构建 Model 基础结构
    val stageList = mutableListOf<Stage>()
    val model = Model(
        name = yamlInput.yaml.name ?: yamlInput.pipelineInfo?.pipelineName ?: "",
        desc = yamlInput.yaml.desc ?: yamlInput.pipelineInfo?.pipelineDesc ?: "",
        stages = stageList,
        labels = emptyList(),
        instanceFromTemplate = false,
        pipelineCreator = yamlInput.pipelineInfo?.creator ?: yamlInput.userId
    )

    val stageIndex = AtomicInteger(0)

    // 3. 构建 Trigger Stage
    if (!yamlInput.yaml.checkForTemplateUse()) {
        stageList.add(modelStage.yaml2TriggerStage(yamlInput, stageIndex.incrementAndGet()))
    }

    // 4. 构建普通 Stage
    formatStage(yamlInput, stageList, stageIndex)

    // 5. 构建 Finally Stage
    formatFinally(yamlInput, stageList, stageIndex.incrementAndGet())

    // 6. 处理模板引用
    formatTemplate(yamlInput, model)

    // 7. 后置切面处理
    yamlInput.aspectWrapper.setModel4Model(model, AFTER)

    return model
}
```

### yaml2Setting 实现

```kotlin
fun yaml2Setting(yamlInput: YamlTransferInput): PipelineSetting {
    val yaml = yamlInput.yaml
    return PipelineSetting(
        projectId = yamlInput.pipelineInfo?.projectId ?: "",
        pipelineId = yamlInput.pipelineInfo?.pipelineId ?: "",
        buildNumRule = yaml.customBuildNum,
        pipelineName = yaml.name ?: yamlInput.yamlFileName ?: yamlInput.pipelineInfo?.pipelineName ?: "",
        desc = yaml.desc ?: yamlInput.pipelineInfo?.pipelineDesc ?: "",

        // 并发控制
        concurrencyGroup = yaml.concurrency?.group ?: PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT,
        concurrencyCancelInProgress = yaml.concurrency?.cancelInProgress ?: false,
        runLockType = when {
            yaml.disablePipeline == true -> PipelineRunLockType.LOCK
            yaml.concurrency?.group != null -> PipelineRunLockType.GROUP_LOCK
            else -> PipelineRunLockType.MULTIPLE
        },
        waitQueueTimeMinute = yaml.concurrency?.queueTimeoutMinutes ?: DEFAULT_WAIT_QUEUE_TIME_MINUTE,
        maxQueueSize = yaml.concurrency?.queueLength ?: DEFAULT_PIPELINE_SETTING_MAX_QUEUE_SIZE,
        maxConRunningQueueSize = yaml.concurrency?.maxParallel ?: PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX,

        // 标签和方言
        labels = yaml2Labels(yamlInput),
        pipelineAsCodeSettings = yamlSyntaxDialect2Setting(yaml.syntaxDialect),

        // 通知订阅
        successSubscriptionList = yamlNotice2Setting(
            projectId = yamlInput.projectCode,
            notices = yaml.notices?.filter { it.checkNotifyForSuccess() }
        ),
        failSubscriptionList = yamlNotice2Setting(
            projectId = yamlInput.projectCode,
            notices = yaml.notices?.filter { it.checkNotifyForFail() }
        ),

        failIfVariableInvalid = yaml.failIfVariableInvalid.nullIfDefault(false),
        buildCancelPolicy = BuildCancelPolicy.codeParse(yaml.cancelPolicy)
    )
}
```

---

## ElementTransfer（元素转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/ElementTransfer.kt`

```kotlin
@Component
class ElementTransfer @Autowired constructor(
    val client: Client,
    val creator: TransferCreator,
    val transferCache: TransferCacheService,
    val triggerTransfer: TriggerTransfer
) {
    fun yaml2Triggers(yamlInput: YamlTransferInput, elements: MutableList<Element>)
    fun baseTriggers2yaml(elements: List<Element>, aspectWrapper: PipelineTransferAspectWrapper): TriggerOn?
    fun scmTriggers2Yaml(elements: List<Element>, projectId: String, aspectWrapper: PipelineTransferAspectWrapper): Map<ScmType, List<TriggerOn>>
    fun yaml2Step(step: Step, job: Job, yamlInput: YamlTransferInput): Element
    fun element2YamlStep(element: Element, projectId: String): PreStep
}
```

### yaml2Step 实现

```kotlin
fun yaml2Step(step: Step, job: Job, yamlInput: YamlTransferInput): Element {
    return when {
        step is PreCheckoutStep -> GitCheckoutElement(
            name = step.name ?: "Checkout",
            repositoryHashId = step.with?.get("repository") as? String,
            branchName = step.with?.get("ref") as? String
        )
        step.uses != null -> {
            val (atomCode, version) = parseAtomCodeAndVersion(step.uses!!)
            MarketBuildAtomElement(
                name = step.name ?: atomCode,
                atomCode = atomCode, version = version,
                data = step.with ?: emptyMap()
            )
        }
        step.run != null -> when (job.runsOn) {
            JobRunsOnType.WINDOWS -> WindowsScriptElement(
                name = step.name ?: "Script",
                script = step.run!!, scriptType = BuildScriptType.BAT
            )
            else -> LinuxScriptElement(
                name = step.name ?: "Script",
                script = step.run!!, scriptType = BuildScriptType.SHELL
            )
        }
        step.template != null -> StepTemplateElement(
            name = step.name ?: "Template",
            templatePath = step.template!!,
            parameters = step.with ?: emptyMap()
        )
        else -> throw ModelCreateException("Invalid step definition")
    }
}
```

---

## StageTransfer / ContainerTransfer

### StageTransfer

```kotlin
@Component
class StageTransfer @Autowired constructor(
    val containerTransfer: ContainerTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer
) {
    fun yaml2TriggerStage(yamlInput: YamlTransferInput, stageIndex: Int): Stage
    fun yaml2NormalStage(stage: IStage, yamlInput: YamlTransferInput, stageIndex: Int): Stage
    fun stage2YamlStage(stage: Stage, projectId: String): PreStage
}
```

### ContainerTransfer

处理 YAML Job 的 `runs-on` 到 Model Container 类型的映射（见主文档的 Container 类型映射表）。

---

## TriggerTransfer（触发器转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/TriggerTransfer.kt`

### 支持的触发器类型

```kotlin
enum class TriggerType {
    BASE,        // 基础触发器（手动、定时、远程）
    CODE_GIT,    // Git 触发器
    CODE_TGIT,   // TGit 触发器
    GITHUB,      // GitHub 触发器
    CODE_SVN,    // SVN 触发器
    CODE_P4,     // Perforce 触发器
    CODE_GITLAB, // GitLab 触发器
    SCM_GIT,     // SCM Git 触发器
    SCM_SVN      // SCM SVN 触发器
}
```

### 转换方法

```kotlin
@Component
class TriggerTransfer {
    fun yaml2TriggerBase(yamlInput: YamlTransferInput, triggerOn: TriggerOn, elements: MutableList<Element>)
    fun yaml2TriggerGit(triggerOn: TriggerOn, elements: MutableList<Element>)
    fun yaml2TriggerGithub(triggerOn: TriggerOn, elements: MutableList<Element>)
    fun timer2YamlTrigger(element: TimerTriggerElement): SchedulesRule
    fun git2YamlTriggerOn(
        elements: List<WebHookTriggerElementChanger>,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper,
        defaultName: String
    ): List<TriggerOn>
}
```

---

## 数据模型

### YamlTransferInput

```kotlin
data class YamlTransferInput(
    val userId: String,
    val projectCode: String,
    val yaml: IPreTemplateScriptBuildYamlParser,
    val pipelineInfo: PipelineInfo? = null,
    val yamlFileName: String? = null,
    val defaultScmType: ScmType = ScmType.CODE_GIT,
    val aspectWrapper: PipelineTransferAspectWrapper = PipelineTransferAspectWrapper()
)
```

### ModelTransferInput

```kotlin
data class ModelTransferInput(
    val userId: String,
    val projectCode: String,
    val model: Model,
    val yamlVersion: YamlVersion = YamlVersion.V2_0,
    val checkPermission: Boolean = true,
    val defaultScmType: ScmType = ScmType.CODE_GIT,
    val aspectWrapper: PipelineTransferAspectWrapper = PipelineTransferAspectWrapper()
)
```

### TransferMark（位置标记）

```kotlin
data class TransferMark(
    val startMark: Mark,
    val endMark: Mark
) {
    data class Mark(val line: Int, val column: Int)
}
```

### NodeIndex（节点索引）

```kotlin
data class NodeIndex(
    val key: String? = null,
    val index: Int? = null,
    val next: NodeIndex? = null
)

// 示例：定位 stages[0].jobs.compile.steps[2]
val index = NodeIndex(
    key = "stages",
    next = NodeIndex(index = 0, next = NodeIndex(
        key = "jobs", next = NodeIndex(
            key = "compile", next = NodeIndex(
                key = "steps", next = NodeIndex(index = 2)
            )
        )
    ))
)
```

---

## 切面系统（PipelineTransferAspectWrapper）

```kotlin
class PipelineTransferAspectWrapper {
    enum class AspectType { BEFORE, AFTER }

    fun setYaml4Yaml(yaml: IPreTemplateScriptBuildYamlParser, type: AspectType)
    fun setModel4Model(model: Model, type: AspectType)
    fun setYamlTriggerOn(triggerOn: TriggerOn, type: AspectType)
    fun setModelElement4Model(element: Element, type: AspectType)
}
```

使用场景：自定义校验、注入额外转换处理、记录转换日志、插件化扩展。

---

## YAML 版本接口

```kotlin
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "version",
    defaultImpl = PreTemplateScriptBuildYamlParser::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlV3Parser::class, name = YamlVersion.V3),
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlParser::class, name = YamlVersion.V2)
)
interface IPreTemplateScriptBuildYamlParser : YamlVersionParser {
    val version: String?
    val name: String?
    val desc: String?
    val label: List<String>?
    val notices: List<Notices>?
    var concurrency: Concurrency?
    var disablePipeline: Boolean?
    var recommendedVersion: RecommendedVersion?
    var customBuildNum: String?
    var syntaxDialect: String?
    var failIfVariableInvalid: Boolean?
    var cancelPolicy: String?

    fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlIParser)
    fun formatVariables(): Map<String, Variable>
    fun formatTriggerOn(default: ScmType): List<Pair<TriggerType, TriggerOn>>
    fun formatStages(): List<IStage>
    fun formatFinallyStage(): List<IJob>
    fun formatResources(): Resources?
    fun formatExtends(): Extends?
    fun templateFilter(): ITemplateFilter
    fun settingGroups(): List<PipelineSettingGroupType>?
    fun checkForTemplateUse(): Boolean
}
```
