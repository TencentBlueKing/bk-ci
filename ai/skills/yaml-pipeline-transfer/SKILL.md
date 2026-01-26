---
name: yaml-pipeline-transfer
description: YAML 流水线转换指南，涵盖 YAML 与 Model 双向转换、PAC（Pipeline as Code）实现、模板引用、触发器配置。当用户需要解析 YAML 流水线、实现 PAC 模式、处理流水线模板或进行 YAML 语法校验时使用。
---

# Skill 22: YAML 流水线转换指南

## 概述

YAML 流水线转换是 BK-CI 的核心功能之一，支持 Pipeline as Code（PAC）模式。本 Skill 详细介绍 YAML 与 Model 之间的双向转换机制、模板系统、触发器配置等关键技术。

## 触发条件

当用户需要实现以下功能时，使用此 Skill：
- YAML 流水线解析与转换
- Model 转换为 YAML
- PAC（Pipeline as Code）实现
- 流水线模板引用
- YAML 语法校验
- 自定义 YAML 处理逻辑

---

## 核心组件架构

### 1. TransferMapper（转换映射器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/TransferMapper.kt`

**职责**：YAML 与对象之间的序列化/反序列化核心引擎

#### 关键方法

```kotlin
object TransferMapper {
    // YAML 字符串转对象
    fun <T> to(str: String): T
    
    // 对象转 YAML 字符串
    fun toYaml(bean: Any): String
    
    // 任意对象转换
    fun <T> anyTo(any: Any?): T
    
    // 格式化 YAML
    fun formatYaml(yaml: String): String
    
    // 合并 YAML（保留注释和锚点）
    fun mergeYaml(old: String, new: String): String
    
    // 获取 YAML 第一层级的坐标定位
    fun getYamlLevelOneIndex(yaml: String): Map<String, TransferMark>
    
    // YAML 节点索引
    fun indexYaml(yaml: String, line: Int, column: Int): NodeIndex?
    
    // 标记 YAML 节点位置
    fun markYaml(index: NodeIndex, yaml: String): TransferMark?
    
    // 获取 YAML 工厂
    fun getYamlFactory(): Yaml
    
    // 获取 ObjectMapper
    fun getObjectMapper(): ObjectMapper
}
```

#### 自定义特性

**1. 自定义字符串引号检查器**

解决 YAML `on` 关键字的特殊用法：

```kotlin
class CustomStringQuotingChecker : StringQuotingChecker() {
    override fun needToQuoteName(name: String): Boolean {
        // 自定义逻辑：on 关键字不加引号
        if (name == "on") return false
        return reservedKeyword(name) || looksLikeYAMLNumber(name)
    }
    
    // 检测十六进制数字（0x开头需要加引号）
    private fun looksLikeHexNumber(value: String): Boolean {
        if (value.length < 3) return false
        return value.startsWith("0x", ignoreCase = true)
    }
}
```

**2. 自定义 YAML 生成器**

去除换行符前的尾随空格，支持 YAML Block 输出：

```kotlin
override fun writeString(text: String) {
    super.writeString(removeTrailingSpaces(text))
}

private fun removeTrailingSpaces(text: String): String {
    val result = StringBuilder(text.length)
    // 逐行处理，移除每行末尾的空格
    // ...
    return result.toString()
}
```

**3. 锚点（Anchor）管理**

```kotlin
// 收集 YAML 中的所有锚点
private fun anchorNode(node: Node, anchors: MutableMap<String, Node>)

// 替换相同节点为锚点引用
private fun replaceAnchor(node: Node, anchors: Map<String, Node>)

// 自定义锚点生成器（保持原命名）
class CustomAnchorGenerator : AnchorGenerator {
    override fun nextAnchor(node: Node): String {
        return node.anchor  // 不重命名
    }
}
```

**4. mergeYaml 功能**

智能合并两个 YAML，保留注释和锚点：

```kotlin
fun mergeYaml(old: String, new: String): String {
    if (old.isBlank()) return new
    
    val oldE = getYamlFactory().parse(old.reader()).toList()
    val newE = getYamlFactory().parse(new.reader()).toMutableList()
    
    // 使用 Myers Diff 算法计算差异
    val patch = DiffUtils.diff(oldE, newE, MeyersDiffWithLinearSpace.factory().create())
    
    // 处理注释和锚点
    for (delta in patch.deltas) {
        when (delta.type) {
            DeltaType.DELETE -> {
                // 保留源文件的注释
                val sourceComment = checkCommentEvent(delta.source.lines)
                if (sourceComment.isNotEmpty()) {
                    newE.addAll(delta.target.position, sourceComment)
                }
            }
            DeltaType.INSERT -> {
                // 保留锚点信息
                anchorChecker[delta.source.position]?.let { checker ->
                    // 恢复锚点
                }
            }
        }
    }
    
    // 重建节点，恢复锚点引用
    val newNode = eventsComposer(newE).singleNode
    replaceAnchor(newNode, anchorNodes)
    
    return getYamlFactory().serialize(newNode)
}
```

---

### 2. ModelTransfer（Model 转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/ModelTransfer.kt`

**职责**：YAML ↔ Pipeline Model 的核心转换逻辑

#### 关键方法

```kotlin
@Component
class ModelTransfer @Autowired constructor(
    val client: Client,
    val modelStage: StageTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer,
    val transferCache: TransferCacheService
) {
    // YAML 转 Model
    fun yaml2Model(yamlInput: YamlTransferInput): Model
    
    // YAML 转 Setting
    fun yaml2Setting(yamlInput: YamlTransferInput): PipelineSetting
    
    // YAML 转 Labels
    fun yaml2Labels(yamlInput: YamlTransferInput): List<String>
    
    // Model 转 YAML
    fun model2Yaml(input: ModelTransferInput): PreTemplateScriptBuildYamlParser
}
```

#### yaml2Model 实现流程

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

#### yaml2Setting 实现

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
        
        // 其他配置
        failIfVariableInvalid = yaml.failIfVariableInvalid.nullIfDefault(false),
        buildCancelPolicy = BuildCancelPolicy.codeParse(yaml.cancelPolicy)
    )
}
```

---

### 3. ElementTransfer（元素转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/ElementTransfer.kt`

**职责**：YAML Step ↔ Model Element 转换

#### 元素类型映射

| YAML Step | Model Element | 说明 |
|-----------|---------------|------|
| `uses: checkout@v2` | `GitCheckoutElement` | 代码检出 |
| `uses: <atomCode>@v*` | `MarketBuildAtomElement` | 研发商店插件 |
| `run: <script>` | `LinuxScriptElement` / `WindowsScriptElement` | 脚本执行 |
| `uses: manual-review@v*` | `ManualReviewUserTaskElement` | 人工审核 |
| `template: <path>` | `StepTemplateElement` | 步骤模板 |

#### 关键方法

```kotlin
@Component
class ElementTransfer @Autowired constructor(
    val client: Client,
    val creator: TransferCreator,
    val transferCache: TransferCacheService,
    val triggerTransfer: TriggerTransfer
) {
    // YAML 转触发器
    fun yaml2Triggers(yamlInput: YamlTransferInput, elements: MutableList<Element>)
    
    // 触发器转 YAML
    fun baseTriggers2yaml(elements: List<Element>, aspectWrapper: PipelineTransferAspectWrapper): TriggerOn?
    
    // SCM 触发器转 YAML
    fun scmTriggers2Yaml(elements: List<Element>, projectId: String, aspectWrapper: PipelineTransferAspectWrapper): Map<ScmType, List<TriggerOn>>
    
    // YAML Step 转 Element
    fun yaml2Step(step: Step, job: Job, yamlInput: YamlTransferInput): Element
    
    // Element 转 YAML Step
    fun element2YamlStep(element: Element, projectId: String): PreStep
}
```

#### yaml2Step 实现示例

```kotlin
fun yaml2Step(step: Step, job: Job, yamlInput: YamlTransferInput): Element {
    return when {
        // checkout 步骤
        step is PreCheckoutStep -> {
            GitCheckoutElement(
                name = step.name ?: "Checkout",
                repositoryHashId = step.with?.get("repository") as? String,
                branchName = step.with?.get("ref") as? String,
                // ... 其他参数
            )
        }
        
        // uses: 插件步骤
        step.uses != null -> {
            val (atomCode, version) = parseAtomCodeAndVersion(step.uses!!)
            MarketBuildAtomElement(
                name = step.name ?: atomCode,
                atomCode = atomCode,
                version = version,
                data = step.with ?: emptyMap()
            )
        }
        
        // run: 脚本步骤
        step.run != null -> {
            when (job.runsOn) {
                JobRunsOnType.WINDOWS -> WindowsScriptElement(
                    name = step.name ?: "Script",
                    script = step.run!!,
                    scriptType = BuildScriptType.BAT
                )
                else -> LinuxScriptElement(
                    name = step.name ?: "Script",
                    script = step.run!!,
                    scriptType = BuildScriptType.SHELL
                )
            }
        }
        
        // template: 步骤模板
        step.template != null -> {
            StepTemplateElement(
                name = step.name ?: "Template",
                templatePath = step.template!!,
                parameters = step.with ?: emptyMap()
            )
        }
        
        else -> throw ModelCreateException("Invalid step definition")
    }
}
```

---

### 4. StageTransfer（Stage 转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/StageTransfer.kt`

**职责**：YAML Stage ↔ Model Stage 转换

#### 关键方法

```kotlin
@Component
class StageTransfer @Autowired constructor(
    val containerTransfer: ContainerTransfer,
    val elementTransfer: ElementTransfer,
    val variableTransfer: VariableTransfer
) {
    // YAML 转 Trigger Stage
    fun yaml2TriggerStage(yamlInput: YamlTransferInput, stageIndex: Int): Stage
    
    // YAML Stage 转 Model Stage
    fun yaml2NormalStage(
        stage: IStage,
        yamlInput: YamlTransferInput,
        stageIndex: Int
    ): Stage
    
    // Model Stage 转 YAML Stage
    fun stage2YamlStage(stage: Stage, projectId: String): PreStage
}
```

---

### 5. ContainerTransfer（Container 转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/ContainerTransfer.kt`

**职责**：YAML Job ↔ Model Container 转换

#### Container 类型映射

| YAML Job runs-on | Model Container | 说明 |
|------------------|-----------------|------|
| `linux` | `VMBuildContainer` | Linux 虚拟机 |
| `windows` | `VMBuildContainer` | Windows 虚拟机 |
| `macos` | `VMBuildContainer` | macOS 虚拟机 |
| `agent-id: <id>` | `ThirdPartyAgentIdContainer` | 第三方构建机（ID） |
| `agent-name: <name>` | `ThirdPartyAgentNameContainer` | 第三方构建机（名称） |
| `pool: <pool>` | `ThirdPartyAgentNameContainer` | 构建池 |
| `self-hosted: true` | `NormalContainer` | 自托管环境 |

---

### 6. TriggerTransfer（触发器转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/TriggerTransfer.kt`

**职责**：处理各种触发器的转换逻辑

#### 支持的触发器类型

```kotlin
enum class TriggerType {
    BASE,           // 基础触发器（手动、定时、远程）
    CODE_GIT,       // Git 触发器
    CODE_TGIT,      // TGit 触发器
    GITHUB,         // GitHub 触发器
    CODE_SVN,       // SVN 触发器
    CODE_P4,        // Perforce 触发器
    CODE_GITLAB,    // GitLab 触发器
    SCM_GIT,        // SCM Git 触发器
    SCM_SVN         // SCM SVN 触发器
}
```

#### 触发器转换方法

```kotlin
@Component
class TriggerTransfer {
    // 基础触发器（手动、定时、远程）
    fun yaml2TriggerBase(yamlInput: YamlTransferInput, triggerOn: TriggerOn, elements: MutableList<Element>)
    
    // Git 触发器
    fun yaml2TriggerGit(triggerOn: TriggerOn, elements: MutableList<Element>)
    
    // GitHub 触发器
    fun yaml2TriggerGithub(triggerOn: TriggerOn, elements: MutableList<Element>)
    
    // 定时触发器转 YAML
    fun timer2YamlTrigger(element: TimerTriggerElement): SchedulesRule
    
    // Git WebHook 转 YAML
    fun git2YamlTriggerOn(
        elements: List<WebHookTriggerElementChanger>,
        projectId: String,
        aspectWrapper: PipelineTransferAspectWrapper,
        defaultName: String
    ): List<TriggerOn>
}
```

---

### 7. VariableTransfer（变量转换器）

**位置**：`common-pipeline-yaml/src/main/kotlin/.../transfer/VariableTransfer.kt`

**职责**：YAML Variable ↔ Model BuildFormProperty 转换

详见 **[流水线变量管理指南 - 变量字段扩展](../pipeline-variable-management/reference/2-extension.md)**

---

## YAML 数据模型

### 版本体系

BK-CI 支持两个 YAML 版本：

| 版本 | 标识 | 说明 |
|------|------|------|
| **v2.0** | `version: v2.0` | 当前主版本 |
| **v3.0** | `version: v3.0` | 增强版本（支持更多特性） |

**接口定义**：

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

### 完整 YAML 结构

```yaml
version: v2.0                    # 版本号（必填）

name: CI Pipeline                # 流水线名称
desc: 持续集成流水线描述           # 描述
label:                           # 标签
  - backend
  - production

# ========== 触发器配置 ==========
on:
  push:                          # 推送触发
    branches:
      - master
      - develop
      - /^feature\/.*/
    paths:                       # 路径过滤
      - src/**
      - build.gradle
    paths-ignore:                # 路径排除
      - docs/**
      - "*.md"
    
  mr:                            # 合并请求触发
    target-branches:
      - master
    action:
      - open
      - update
      - close
    block-mr: true               # 阻塞 MR
    report-commit-check: true    # 上报提交检查
    
  tag:                           # Tag 触发
    tags:
      - /^v.*/
    
  schedules:                     # 定时触发
    - cron: "0 2 * * *"          # Cron 表达式
      always: true               # 总是执行
      branches:
        - master
    - interval:                  # 固定时间触发
        week:
          - Mon
          - Fri
        time-points:
          - "02:00"
          - "14:00"
      
  manual:                        # 手动触发
    enable: true
    use-latest-parameters: true  # 使用最近一次参数
    
  remote:                        # 远程触发
    enable: true

# ========== 变量定义 ==========
variables:
  BUILD_TYPE:                    # 简单变量
    value: release
    readonly: false
    allow-modify-at-startup: true
    as-instance-input: true
    
  DEPLOY_ENV:                    # 枚举变量
    value: prod
    props:
      type: enum
      options:
        - dev
        - test
        - prod
      label: "部署环境"
      description: "选择部署的目标环境"
      
  API_TOKEN:                     # 密码变量
    value: ""
    props:
      type: password
      label: "API Token"
      
  VERSION_NUMBER:                # 数字变量
    value: 1
    props:
      type: number
      min: 1
      max: 100

# ========== 并发控制 ==========
concurrency:
  group: ${{ variables.BUILD_TYPE }}  # 并发组
  cancel-in-progress: true             # 取消进行中的构建
  queue-length: 10                     # 队列长度
  queue-timeout-minutes: 30            # 队列超时（分钟）
  max-parallel: 5                      # 最大并发数

# ========== 资源池配置 ==========
resources:
  repositories:                  # 代码库资源
    - repository: my-repo
      type: github
      name: my-org/my-repo
      ref: main
  pools:                         # 构建池
    - pool: my-pool
      container: linux

# ========== 模板引用 ==========
extends:
  template: templates/base.yml   # 模板路径
  parameters:                    # 模板参数
    BUILD_TYPE: ${{ variables.BUILD_TYPE }}

# ========== Stage 定义 ==========
stages:
  - name: Build                  # Stage 名称
    label:                       # Stage 标签
      - compile
    if: ${{ eq(variables.BUILD_TYPE, 'release') }}  # 执行条件
    if-modify:                   # 路径变更条件
      - src/**
    check-in: manual             # 准入审核
    check-out: manual            # 准出审核
    fast-kill: true              # 快速终止
    jobs:                        # Job 列表
      compile:                   # Job ID
        name: 编译构建            # Job 名称
        runs-on: linux           # 运行环境
        if: success()            # 执行条件
        timeout-minutes: 60      # 超时时间
        continue-on-error: false # 失败继续
        strategy:                # 矩阵策略
          matrix:
            os: [linux, windows]
            node: [14, 16, 18]
          fail-fast: true
        env:                     # 环境变量
          NODE_ENV: production
        steps:                   # 步骤列表
          # Checkout 步骤
          - uses: checkout@v2
            with:
              repository: ${{ resources.repositories.my-repo }}
              ref: ${{ on.push.branch }}
              fetch-depth: 1
              submodules: false
              lfs: false
              enable-git-clean: true
              
          # 脚本步骤
          - name: Build
            run: |
              echo "Building..."
              ./gradlew build
            if: success()
            continue-on-error: false
            timeout-minutes: 30
            retry-times: 3
            
          # 插件步骤
          - name: Upload Artifact
            uses: upload-artifact@v2
            with:
              name: build-output
              path: build/
              retention-days: 7
              
          # 步骤模板
          - template: templates/deploy-step.yml
            parameters:
              env: prod
              
          # 人工审核
          - name: Manual Review
            uses: manual-review@v1
            with:
              desc: "请审核构建产物"
              reviewers:
                - user1
                - user2
              notify-type: [email, wechat]

  - name: Deploy
    label:
      - deployment
    depends-on:                  # 依赖的 Stage
      - Build
    jobs:
      deploy:
        name: 部署
        runs-on:
          pool: prod-pool        # 指定构建池
        steps:
          - name: Download Artifact
            uses: download-artifact@v2
            with:
              name: build-output
              
          - name: Deploy
            run: ./deploy.sh

# ========== Finally Stage ==========
finally:                         # 最终执行（无论成功失败）
  cleanup:
    name: 清理
    runs-on: linux
    if: always()                 # 总是执行
    steps:
      - name: Cleanup
        run: rm -rf temp/

# ========== 通知配置 ==========
notices:
  - notify-type: [email, wechat] # 通知类型
    notify-when: [fail]          # 通知时机
    notify-group: [开发组]        # 通知组
    notify-user: [user1]         # 通知用户
    content: "构建失败，请及时处理"
    title: "流水线失败通知"

# ========== 其他配置 ==========
disable-pipeline: false          # 禁用流水线
custom-build-num: ${{ variables.VERSION_NUMBER }}  # 自定义构建号
syntax-dialect: CLASSIC          # 语法方言（CLASSIC/CONSTRAINT）
fail-if-variable-invalid: false  # 变量无效时失败
cancel-policy: SIMPLE            # 取消策略

# ========== 推荐版本 ==========
recommended-version:
  enabled: true
  version: "1.0.0"
  reason: "稳定版本"
```

---

## 核心概念详解

### 1. YamlTransferInput（转换输入）

```kotlin
data class YamlTransferInput(
    val userId: String,                      // 用户 ID
    val projectCode: String,                 // 项目 ID
    val yaml: IPreTemplateScriptBuildYamlParser,  // YAML 对象
    val pipelineInfo: PipelineInfo? = null,  // 流水线信息
    val yamlFileName: String? = null,        // YAML 文件名
    val defaultScmType: ScmType = ScmType.CODE_GIT,  // 默认 SCM 类型
    val aspectWrapper: PipelineTransferAspectWrapper = PipelineTransferAspectWrapper()  // 切面包装器
)
```

### 2. ModelTransferInput（Model 转换输入）

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

### 3. TransferMark（位置标记）

用于在 YAML 文件中定位节点位置：

```kotlin
data class TransferMark(
    val startMark: Mark,  // 起始位置
    val endMark: Mark     // 结束位置
) {
    data class Mark(
        val line: Int,     // 行号
        val column: Int    // 列号
    )
}
```

### 4. NodeIndex（节点索引）

用于在 YAML AST 中定位节点：

```kotlin
data class NodeIndex(
    val key: String? = null,      // 对象键
    val index: Int? = null,       // 数组索引
    val next: NodeIndex? = null   // 下一级节点
) {
    override fun toString(): String {
        return key ?: "array($index)" + (next?.toString() ?: "")
    }
}
```

**使用示例**：

```kotlin
// 定位 stages[0].jobs.compile.steps[2]
val index = NodeIndex(
    key = "stages",
    next = NodeIndex(
        index = 0,
        next = NodeIndex(
            key = "jobs",
            next = NodeIndex(
                key = "compile",
                next = NodeIndex(
                    key = "steps",
                    next = NodeIndex(
                        index = 2
                    )
                )
            )
        )
    )
)
```

---

## 使用场景与示例

### 场景 1：YAML 导入流水线

```kotlin
@Service
class PipelineYamlService(
    private val modelTransfer: ModelTransfer,
    private val pipelineService: PipelineService
) {
    fun importFromYaml(
        userId: String,
        projectId: String,
        yaml: String,
        yamlFileName: String? = null
    ): Pipeline {
        // 1. 解析 YAML
        val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)
        
        // 2. 校验 YAML
        validateYaml(yamlObject)
        
        // 3. 构建转换输入
        val yamlInput = YamlTransferInput(
            userId = userId,
            projectCode = projectId,
            yaml = yamlObject,
            yamlFileName = yamlFileName
        )
        
        // 4. 转换为 Model
        val model = modelTransfer.yaml2Model(yamlInput)
        val setting = modelTransfer.yaml2Setting(yamlInput)
        
        // 5. 创建流水线
        return pipelineService.createPipeline(
            userId = userId,
            projectId = projectId,
            model = model,
            setting = setting
        )
    }
    
    private fun validateYaml(yaml: IPreTemplateScriptBuildYamlParser) {
        if (yaml.name.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = "YAML_NAME_REQUIRED",
                defaultMessage = "流水线名称不能为空"
            )
        }
        
        val stages = yaml.formatStages()
        if (stages.isEmpty()) {
            throw ErrorCodeException(
                errorCode = "YAML_STAGES_EMPTY",
                defaultMessage = "至少需要定义一个 Stage"
            )
        }
    }
}
```

### 场景 2：流水线导出为 YAML

```kotlin
@Service
class PipelineExportService(
    private val modelTransfer: ModelTransfer,
    private val pipelineService: PipelineService
) {
    fun exportToYaml(
        userId: String,
        projectId: String,
        pipelineId: String,
        yamlVersion: YamlVersion = YamlVersion.V2_0
    ): String {
        // 1. 获取流水线 Model
        val model = pipelineService.getModel(userId, projectId, pipelineId)
        
        // 2. 构建转换输入
        val input = ModelTransferInput(
            userId = userId,
            projectCode = projectId,
            model = model,
            yamlVersion = yamlVersion
        )
        
        // 3. 转换为 YAML 对象
        val yamlObject = modelTransfer.model2Yaml(input)
        
        // 4. 序列化为 YAML 字符串
        return TransferMapper.toYaml(yamlObject)
    }
}
```

### 场景 3：PAC 从代码库同步

```kotlin
@Service
class PacService(
    private val repositoryService: RepositoryService,
    private val pipelineYamlService: PipelineYamlService
) {
    fun syncFromRepo(
        userId: String,
        projectId: String,
        repoUrl: String,
        branch: String = "master",
        yamlPath: String = ".ci/pipeline.yml"
    ): Pipeline {
        // 1. 从代码库拉取 YAML 文件
        val yaml = repositoryService.getFileContent(
            repoUrl = repoUrl,
            branch = branch,
            filePath = yamlPath
        )
        
        // 2. 导入流水线
        return pipelineYamlService.importFromYaml(
            userId = userId,
            projectId = projectId,
            yaml = yaml,
            yamlFileName = yamlPath
        )
    }
    
    fun autoSync(
        userId: String,
        projectId: String,
        pipelineId: String
    ) {
        // 1. 获取流水线的 PAC 配置
        val pacConfig = pipelineService.getPacConfig(pipelineId)
        
        // 2. 检查代码库变更
        val latestCommit = repositoryService.getLatestCommit(
            repoUrl = pacConfig.repoUrl,
            branch = pacConfig.branch,
            filePath = pacConfig.yamlPath
        )
        
        if (latestCommit.sha != pacConfig.lastCommitSha) {
            // 3. 拉取最新 YAML
            val yaml = repositoryService.getFileContent(
                repoUrl = pacConfig.repoUrl,
                branch = pacConfig.branch,
                filePath = pacConfig.yamlPath
            )
            
            // 4. 更新流水线
            val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)
            val yamlInput = YamlTransferInput(
                userId = userId,
                projectCode = projectId,
                yaml = yamlObject,
                pipelineInfo = PipelineInfo(
                    pipelineId = pipelineId,
                    projectId = projectId
                )
            )
            
            val model = modelTransfer.yaml2Model(yamlInput)
            pipelineService.updatePipeline(userId, projectId, pipelineId, model)
            
            // 5. 更新同步记录
            pipelineService.updatePacConfig(pipelineId, pacConfig.copy(
                lastCommitSha = latestCommit.sha,
                lastSyncTime = System.currentTimeMillis()
            ))
        }
    }
}
```

### 场景 4：YAML 合并（保留注释）

```kotlin
@Service
class YamlMergeService {
    fun mergeYaml(
        oldYaml: String,
        newYaml: String
    ): String {
        // 使用 TransferMapper.mergeYaml 保留注释和锚点
        return TransferMapper.mergeYaml(oldYaml, newYaml)
    }
    
    fun updatePipelineYaml(
        pipelineId: String,
        updates: Map<String, Any>
    ): String {
        // 1. 获取当前 YAML
        val currentYaml = pipelineService.getPipelineYaml(pipelineId)
        
        // 2. 解析为对象
        val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(currentYaml)
        
        // 3. 应用更新
        val updatedObject = yamlObject.copy(
            name = updates["name"] as? String ?: yamlObject.name,
            desc = updates["desc"] as? String ?: yamlObject.desc
            // ... 其他字段
        )
        
        // 4. 转换为新 YAML
        val newYaml = TransferMapper.toYaml(updatedObject)
        
        // 5. 合并（保留原 YAML 的注释）
        return TransferMapper.mergeYaml(currentYaml, newYaml)
    }
}
```

### 场景 5：YAML 元素插入（定位操作）

```kotlin
@Service
class YamlElementInsertService {
    fun insertStep(
        yaml: String,
        stageIndex: Int,
        jobId: String,
        stepIndex: Int,
        newStep: PreStep
    ): String {
        // 1. 解析 YAML 为对象
        val yamlObject = TransferMapper.to<ITemplateFilter>(yaml)
        
        // 2. 定位插入位置
        val position = PositionResponse(
            stageIndex = stageIndex,
            jobId = jobId,
            stepIndex = stepIndex,
            type = PositionResponse.PositionType.STEP
        )
        
        // 3. 计算节点索引
        val nodeIndex = TransferMapper.indexYaml(
            position = position,
            pYml = yamlObject,
            yml = newStep,
            type = ElementInsertBody.ElementInsertType.INSERT
        )
        
        // 4. 转换为新 YAML
        val newYaml = TransferMapper.toYaml(yamlObject)
        
        // 5. 合并原 YAML（保留注释）
        return TransferMapper.mergeYaml(yaml, newYaml)
    }
    
    fun updateStepByPosition(
        yaml: String,
        line: Int,
        column: Int,
        updatedStep: PreStep
    ): String {
        // 1. 根据行列号定位节点
        val nodeIndex = TransferMapper.indexYaml(yaml, line, column)
            ?: throw ErrorCodeException(errorCode = "INVALID_POSITION")
        
        // 2. 解析 YAML
        val yamlObject = TransferMapper.to<ITemplateFilter>(yaml)
        
        // 3. 更新对应节点
        // ... 根据 nodeIndex 定位并更新
        
        // 4. 转换并合并
        val newYaml = TransferMapper.toYaml(yamlObject)
        return TransferMapper.mergeYaml(yaml, newYaml)
    }
}
```

---

## 模板系统

### 1. Extends 模板引用

```yaml
# 主 YAML
extends:
  template: templates/base.yml
  parameters:
    BUILD_TYPE: release
    DEPLOY_ENV: prod

variables:
  CUSTOM_VAR: custom-value
```

```yaml
# templates/base.yml
version: v2.0
name: Base Template

variables:
  BUILD_TYPE:
    value: ${{ parameters.BUILD_TYPE }}
  DEPLOY_ENV:
    value: ${{ parameters.DEPLOY_ENV }}

stages:
  - name: Build
    jobs:
      build:
        runs-on: linux
        steps:
          - name: Build
            run: ./build.sh
```

### 2. Step 模板

```yaml
steps:
  - template: templates/deploy-step.yml
    parameters:
      env: ${{ variables.DEPLOY_ENV }}
```

```yaml
# templates/deploy-step.yml
- name: Deploy to ${{ parameters.env }}
  run: |
    echo "Deploying to ${{ parameters.env }}"
    ./deploy.sh --env=${{ parameters.env }}
```

### 3. Job 模板

```yaml
stages:
  - name: Test
    jobs:
      test:
        template: templates/test-job.yml
        parameters:
          node-version: 16
```

### 4. Stage 模板

```yaml
stages:
  - template: templates/build-stage.yml
    parameters:
      platform: linux
```

---

## 表达式系统

### 1. 变量引用

```yaml
# 引用变量
${{ variables.BUILD_TYPE }}

# 引用触发器信息
${{ on.push.branch }}
${{ on.mr.target-branch }}

# 引用资源
${{ resources.repositories.my-repo }}
```

### 2. 函数调用

```kotlin
// 条件判断
if: ${{ eq(variables.BUILD_TYPE, 'release') }}
if: ${{ ne(variables.DEPLOY_ENV, 'prod') }}

// 逻辑运算
if: ${{ and(eq(variables.BUILD_TYPE, 'release'), ne(on.push.branch, 'master')) }}
if: ${{ or(eq(on.push.branch, 'master'), eq(on.push.branch, 'develop')) }}

// 状态判断
if: success()         // 前序步骤成功
if: failure()         // 前序步骤失败
if: always()          // 总是执行
if: cancelled()       // 被取消
```

### 3. 表达式解析

表达式解析在 `ParametersExpression` 和 `IfField` 中处理：

```kotlin
data class IfField(
    val mode: Mode,           // SIMPLE / COMPLEX
    val expression: String    // 表达式字符串
) {
    enum class Mode {
        SIMPLE,    // 简单模式：success(), failure(), always()
        COMPLEX    // 复杂模式：${{ ... }}
    }
}
```

---

## 切面系统（PipelineTransferAspectWrapper）

用于在转换过程中注入自定义逻辑：

```kotlin
class PipelineTransferAspectWrapper {
    enum class AspectType {
        BEFORE,  // 前置处理
        AFTER    // 后置处理
    }
    
    // 设置 YAML 对象（YAML → Model 过程）
    fun setYaml4Yaml(yaml: IPreTemplateScriptBuildYamlParser, type: AspectType)
    
    // 设置 Model 对象（Model → YAML 过程）
    fun setModel4Model(model: Model, type: AspectType)
    
    // 设置触发器
    fun setYamlTriggerOn(triggerOn: TriggerOn, type: AspectType)
    
    // 设置元素
    fun setModelElement4Model(element: Element, type: AspectType)
}
```

**使用场景**：
- 添加自定义校验逻辑
- 注入额外的转换处理
- 记录转换过程日志
- 实现插件化扩展

---

## 最佳实践

### 1. YAML 版本控制

```yaml
# ✅ 推荐：明确指定版本
version: v2.0

# ❌ 不推荐：省略版本（使用默认版本）
```

### 2. 变量命名规范

```yaml
# ✅ 推荐：大写下划线
variables:
  BUILD_TYPE: release
  DEPLOY_ENV: prod

# ❌ 不推荐：驼峰或小写
variables:
  buildType: release
  deployenv: prod
```

### 3. 使用模板复用

```yaml
# ✅ 推荐：提取公共配置到模板
extends:
  template: templates/base.yml

# ❌ 不推荐：每个流水线重复定义
```

### 4. 合理使用条件执行

```yaml
# ✅ 推荐：使用表达式控制执行
stages:
  - name: Deploy
    if: ${{ eq(variables.DEPLOY_ENV, 'prod') }}

# ❌ 不推荐：在脚本中判断
stages:
  - name: Deploy
    jobs:
      deploy:
        steps:
          - run: |
              if [ "$DEPLOY_ENV" == "prod" ]; then
                ./deploy.sh
              fi
```

### 5. 使用 mergeYaml 保留注释

```kotlin
// ✅ 推荐：使用 mergeYaml 保留原 YAML 的注释和锚点
val merged = TransferMapper.mergeYaml(oldYaml, newYaml)

// ❌ 不推荐：直接覆盖（丢失注释）
val newYaml = TransferMapper.toYaml(yamlObject)
```

### 6. 校验 YAML 完整性

```kotlin
// ✅ 推荐：转换前校验
fun importYaml(yaml: String) {
    val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)
    validateYaml(yamlObject)  // 校验
    val model = modelTransfer.yaml2Model(yamlInput)
}

// ❌ 不推荐：不校验直接转换
fun importYaml(yaml: String) {
    val yamlObject = TransferMapper.to<PreTemplateScriptBuildYamlParser>(yaml)
    val model = modelTransfer.yaml2Model(yamlInput)  // 可能抛出异常
}
```

---

## 常见问题

### Q1: 如何处理 YAML 中的特殊字符？

**A**: 使用 `CustomStringQuotingChecker` 自动处理：

```yaml
# 自动加引号
version: "0x123"  # 十六进制数字
name: "yes"       # 布尔关键字

# 不加引号
on:               # on 关键字特殊处理
  push:
```

### Q2: 如何在 YAML 中使用锚点和别名？

**A**: TransferMapper 自动管理锚点：

```yaml
# 定义锚点
defaults: &defaults
  runs-on: linux
  timeout-minutes: 60

# 使用别名
stages:
  - name: Build
    jobs:
      build:
        <<: *defaults
        steps:
          - run: ./build.sh
```

### Q3: 如何扩展自定义的 YAML 字段？

**A**: 通过继承 `IPreTemplateScriptBuildYamlParser` 并使用 `@JsonSubTypes`：

```kotlin
@JsonSubTypes.Type(value = CustomYamlParser::class, name = "v4.0")
data class CustomYamlParser(
    // 继承所有字段
    override val version: String?,
    override val name: String?,
    // ... 其他字段
    
    // 新增自定义字段
    val customField: String?
) : IPreTemplateScriptBuildYamlParser {
    // 实现必要方法
}
```

### Q4: 如何处理大型 YAML 文件的性能问题？

**A**:
1. 使用流式解析（`getYamlFactory().parse()`）
2. 缓存已解析的模板（`TransferCacheService`）
3. 分批处理 Stage/Job/Step
4. 异步转换非关键路径

### Q5: 如何调试 YAML 转换问题？

**A**:
```kotlin
// 1. 启用 Debug 日志
logger.debug("YAML Input: $yaml")
logger.debug("YAML Object: ${JsonUtil.toJson(yamlObject)}")

// 2. 使用切面记录转换过程
val aspectWrapper = PipelineTransferAspectWrapper()
aspectWrapper.setYaml4Yaml(yamlObject, BEFORE) { yaml ->
    logger.info("Before YAML: ${TransferMapper.toYaml(yaml)}")
}

// 3. 分步转换，定位问题
val model = modelTransfer.yaml2Model(yamlInput)
logger.info("Model: ${JsonUtil.toJson(model)}")

// 4. 使用 getYamlLevelOneIndex 检查结构
val index = TransferMapper.getYamlLevelOneIndex(yaml)
logger.info("YAML Index: $index")
```

---

## 检查清单

在实现 YAML 转换功能前，确认：

- [ ] 确定 YAML 版本（v2.0 / v3.0）
- [ ] 了解 YAML 与 Model 的映射关系
- [ ] 确认需要支持的触发器类型
- [ ] 确认需要支持的元素类型
- [ ] 确认是否需要模板引用
- [ ] 确认是否需要保留注释和锚点（使用 mergeYaml）
- [ ] 添加 YAML 校验逻辑
- [ ] 添加异常处理（捕获 `YamlFormatException`、`ModelCreateException`）
- [ ] 添加单元测试覆盖转换逻辑
- [ ] 考虑性能优化（缓存、异步）
- [ ] 添加日志记录（Debug 级别）

---

## 相关 Skills

- [流水线变量管理](../pipeline-variable-management/SKILL.md) - 变量转换逻辑（参考 reference/2-extension.md）
- [01-后端微服务开发](../01-backend-microservice-development/SKILL.md) - 微服务架构
- [27-设计模式](../27-design-patterns/SKILL.md) - 工厂模式、策略模式

---

## 相关文件路径

### 核心转换类
- `common-pipeline-yaml/src/main/kotlin/.../transfer/TransferMapper.kt` - 转换映射器
- `common-pipeline-yaml/src/main/kotlin/.../transfer/ModelTransfer.kt` - Model 转换
- `common-pipeline-yaml/src/main/kotlin/.../transfer/ElementTransfer.kt` - 元素转换
- `common-pipeline-yaml/src/main/kotlin/.../transfer/StageTransfer.kt` - Stage 转换
- `common-pipeline-yaml/src/main/kotlin/.../transfer/ContainerTransfer.kt` - Container 转换
- `common-pipeline-yaml/src/main/kotlin/.../transfer/TriggerTransfer.kt` - 触发器转换
- `common-pipeline-yaml/src/main/kotlin/.../transfer/VariableTransfer.kt` - 变量转换

### YAML 模型定义
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/PreTemplateScriptBuildYamlParser.kt` - v2.0 YAML 模型
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/PreTemplateScriptBuildYamlV3Parser.kt` - v3.0 YAML 模型
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/Variable.kt` - 变量模型
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/Concurrency.kt` - 并发控制
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/Notices.kt` - 通知配置
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/on/TriggerOn.kt` - 触发器定义
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/stage/Stage.kt` - Stage 定义
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/job/Job.kt` - Job 定义
- `common-pipeline-yaml/src/main/kotlin/.../v3/models/step/Step.kt` - Step 定义

### 测试用例
- `common-pipeline-yaml/src/test/kotlin/.../transfer/MergeYamlTest.kt` - YAML 合并测试
- `common-pipeline-yaml/src/test/kotlin/.../parsers/template/YamlTemplateTest.kt` - 模板测试
- `common-pipeline-yaml/src/test/resources/samples/` - YAML 示例文件

### JSON Schema
- `common-pipeline-yaml/src/main/resources/schema/V3_0/ci.json` - v3.0 Schema
- `common-pipeline-yaml/src/main/resources/schema/V2_0/ci.json` - v2.0 Schema

---

## 总结

YAML 流水线转换是 BK-CI 实现 Pipeline as Code 的核心技术，涉及：

1. **双向转换**：YAML ↔ Model 的完整转换链路
2. **智能合并**：保留注释和锚点的 YAML 合并
3. **模板系统**：支持 Extends、Step/Job/Stage 模板
4. **表达式系统**：变量引用、函数调用、条件判断
5. **切面扩展**：支持自定义转换逻辑注入
6. **节点定位**：支持精确的 YAML 节点操作

遵循本指南可确保 YAML 转换的正确性、可维护性和性能。
