---
name: 24-pipeline-variable-lifecycle
description: 流水线变量生命周期管理指南，涵盖变量的创建、初始化、动态更新、存储、传递和查询的完整生命周期。当用户开发变量功能、处理变量传递、调试变量问题或理解变量作用域时使用。
author: <NAME>
---

# 24-pipeline-variable-lifecycle

## Skill 说明

流水线变量生命周期管理指南 - 涵盖变量的创建、初始化、动态更新、存储、传递和查询的完整生命周期。

---

## 1. 变量生命周期概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        流水线变量生命周期全景图                               │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   用户触发构建   │
                              └────────┬────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段一：变量创建与初始化                                                     │
│                                                                              │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐           │
│  │ 系统预置变量    │   │ 用户启动参数    │   │ 流水线定义变量  │           │
│  │ BK_CI_BUILD_ID  │ + │ 手动输入参数    │ + │ params.xxx      │           │
│  │ BK_CI_PIPELINE  │   │ API 传入参数    │   │ 默认值          │           │
│  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘           │
│           │                     │                     │                     │
│           └─────────────────────┼─────────────────────┘                     │
│                                 ▼                                           │
│                    ┌─────────────────────────┐                              │
│                    │ StartBuildContext.init() │                             │
│                    │ 合并所有变量来源         │                             │
│                    └────────────┬────────────┘                              │
│                                 │                                           │
│                                 ▼                                           │
│                    ┌─────────────────────────┐                              │
│                    │ T_PIPELINE_BUILD_VAR    │                              │
│                    │ 批量写入数据库           │                             │
│                    └─────────────────────────┘                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段二：变量动态更新（执行过程中）                                           │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Worker 端 (构建机)                                                   │   │
│  │                                                                      │   │
│  │  Task 执行 ──▶ 插件输出变量 ──▶ output.json ──▶ completeTask API    │   │
│  │                  outputs:                                            │   │
│  │                    key1: value1                                      │   │
│  │                    key2: value2                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                       │                                     │
│                                       ▼                                     │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │ Engine 端 (服务端)                                                    │   │
│  │                                                                       │   │
│  │  EngineVMBuildService.buildCompleteTask()                            │   │
│  │      │                                                                │   │
│  │      ├──▶ 获取 Redis 分布式锁 (PipelineBuildVarLock)                 │   │
│  │      ├──▶ BuildVariableService.batchUpdateVariable()                 │   │
│  │      └──▶ PipelineBuildVarDao.batchUpdate()                          │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段三：变量传递                                                             │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Job 间传递                                                           │   │
│  │                                                                      │   │
│  │  Job-A 输出: steps.<stepId>.outputs.<key>                           │   │
│  │      │                                                               │   │
│  │      ▼                                                               │   │
│  │  Job-B 读取: ${{ jobs.<jobId>.steps.<stepId>.outputs.<key> }}       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 父子流水线传递                                                       │   │
│  │                                                                      │   │
│  │  父流水线: SubPipelineStartUpService.callPipelineStartup()          │   │
│  │      │                                                               │   │
│  │      ├──▶ PIPELINE_START_PARENT_PROJECT_ID                          │   │
│  │      ├──▶ PIPELINE_START_PARENT_PIPELINE_ID                         │   │
│  │      ├──▶ PIPELINE_START_PARENT_BUILD_ID                            │   │
│  │      └──▶ 用户指定的传递参数                                        │   │
│  │      │                                                               │   │
│  │      ▼                                                               │   │
│  │  子流水线: 通过 getAllVariable() 读取                                │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段四：变量读取与表达式解析                                                 │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 表达式语法                                                           │   │
│  │                                                                      │   │
│  │  ${{ variables.xxx }}      → 流水线变量                              │   │
│  │  ${{ ci.xxx }}             → CI 预置变量                             │   │
│  │  ${{ settings.xxx }}       → 流水线设置                              │   │
│  │  ${{ jobs.xxx.outputs }}   → Job 输出                                │   │
│  │  ${{ steps.xxx.outputs }}  → Step 输出                               │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                       │                                     │
│                                       ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 解析流程                                                             │   │
│  │                                                                      │   │
│  │  EnvReplacementParser.parse()                                        │   │
│  │      │                                                               │   │
│  │      ├──▶ 检测 ${{ }} 表达式                                        │   │
│  │      ├──▶ ExpressionParser.evaluateByMap() 计算表达式               │   │
│  │      └──▶ 返回替换后的值                                            │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 变量存储机制

### 2.1 数据库表结构

```sql
-- 流水线构建变量表
CREATE TABLE IF NOT EXISTS `T_PIPELINE_BUILD_VAR` (
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `KEY` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '变量名',
  `VALUE` varchar(4000) DEFAULT NULL COMMENT '变量值（限制4000字符）',
  `PROJECT_ID` varchar(64) DEFAULT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(64) DEFAULT NULL COMMENT '流水线ID',
  `VAR_TYPE` VARCHAR(64) COMMENT '变量类型',
  `READ_ONLY` bit(1) DEFAULT NULL COMMENT '是否只读',
  PRIMARY KEY (`BUILD_ID`,`KEY`),
  KEY `IDX_SEARCH_BUILD_ID` (`PROJECT_ID`,`PIPELINE_ID`, `KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线变量表';
```

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `BUILD_ID` | varchar(34) | 构建 ID，主键之一 |
| `KEY` | varchar(255) | 变量名，区分大小写 |
| `VALUE` | varchar(4000) | 变量值，最大 4000 字符 |
| `PROJECT_ID` | varchar(64) | 项目 ID，用于索引优化 |
| `PIPELINE_ID` | varchar(64) | 流水线 ID |
| `VAR_TYPE` | varchar(64) | 变量类型（见下文枚举） |
| `READ_ONLY` | bit(1) | 是否只读，只读变量不可被覆盖 |

### 2.2 核心类

| 类名 | 文件路径 | 职责 |
|------|----------|------|
| `PipelineBuildVarDao` | `biz-base/.../engine/dao/PipelineBuildVarDao.kt` | DAO 层，直接操作数据库 |
| `BuildVariableService` | `biz-base/.../service/BuildVariableService.kt` | Service 层，变量业务逻辑 |
| `PipelineBuildVarLock` | `biz-base/.../control/lock/PipelineBuildVarLock.kt` | Redis 分布式锁 |

### 2.3 DAO 层核心方法

```kotlin
// 文件: biz-base/.../engine/dao/PipelineBuildVarDao.kt

class PipelineBuildVarDao {
    
    // 批量保存变量（构建启动时使用）
    fun batchSave(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: List<BuildParameters>
    )
    
    // 批量更新变量（Task 完成时使用）
    fun batchUpdate(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: List<BuildParameters>
    )
    
    // 获取所有变量（返回 Map<String, String>）
    fun getVars(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        keys: Set<String>? = null
    ): Map<String, String>
    
    // 获取带类型的变量
    fun getVarsWithType(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String? = null
    ): List<BuildParameters>
    
    // 模糊查询变量（用于 BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_* 等前缀变量）
    fun fetchVarByLikeKey(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        likeKey: String
    ): MutableMap<String, String>
    
    // 删除变量
    fun deleteBuildVar(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        varName: String? = null
    ): Int
}
```

### 2.4 Service 层核心方法

```kotlin
// 文件: biz-base/.../service/BuildVariableService.kt

@Service
class BuildVariableService {
    
    // 构建启动时批量保存（无锁，因为此时不存在并发）
    fun startBuildBatchSaveWithoutThreadSafety(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: List<BuildParameters>
    )
    
    // 带锁保存变量（执行过程中使用）
    fun saveVariable(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        name: String,
        value: Any
    )
    
    // 批量更新变量（Task 完成时使用）
    fun batchUpdateVariable(
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: List<BuildParameters>
    )
    
    // 获取所有变量
    fun getAllVariable(
        projectId: String,
        pipelineId: String,
        buildId: String,
        keys: Set<String>? = null
    ): Map<String, String>
    
    // 获取单个变量
    fun getVariable(
        projectId: String,
        pipelineId: String,
        buildId: String,
        varName: String
    ): String?
    
    // 模板变量替换
    fun replaceTemplate(
        projectId: String,
        buildId: String,
        template: String
    ): String
}
```

### 2.5 并发控制：分布式锁

```kotlin
// 文件: biz-base/.../control/lock/PipelineBuildVarLock.kt

class PipelineBuildVarLock(
    redisOperation: RedisOperation, 
    buildId: String, 
    name: String? = null
) : RedisLock(
    redisOperation = redisOperation,
    lockKey = "pipelineBuildVar:$buildId" + (name?.let { ":$it" } ?: ""),
    expiredTimeInSeconds = 10L  // 锁过期时间 10 秒
)
```

**使用场景**
- 多个 Task 并发完成时，需要更新同一个构建的变量
- 防止变量覆盖冲突

---

## 3. 变量创建与初始化

### 3.1 变量来源

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           变量来源优先级（从低到高）                          │
└─────────────────────────────────────────────────────────────────────────────┘

优先级 1 (最低): 流水线定义的默认值
    │
    ▼
优先级 2: 系统预置变量 (BK_CI_*)
    │
    ▼
优先级 3: 用户启动参数 (手动输入/API 传入)
    │
    ▼
优先级 4 (最高): 执行过程中插件输出的变量
```

### 3.2 系统预置变量

系统预置变量定义在 `VariableType` 枚举和 `Constants` 常量类中。

| 分类 | 变量示例 | 说明 |
|------|----------|------|
| **基础构建** | `BK_CI_BUILD_ID`, `BK_CI_BUILD_NUM`, `BK_CI_BUILD_URL` | 构建标识和链接 |
| **流水线** | `BK_CI_PIPELINE_ID`, `BK_CI_PIPELINE_NAME`, `BK_CI_PIPELINE_VERSION` | 流水线信息 |
| **项目** | `BK_CI_PROJECT_NAME`, `BK_CI_PROJECT_NAME_CN` | 项目标识 |
| **触发信息** | `BK_CI_START_TYPE`, `BK_CI_START_USER_NAME`, `BK_CI_START_CHANNEL` | 触发来源 |
| **父子流水线** | `BK_CI_PARENT_PROJECT_ID`, `BK_CI_PARENT_PIPELINE_ID`, `BK_CI_PARENT_BUILD_ID` | 父流水线信息 |
| **执行上下文** | `BK_CI_BUILD_JOB_ID`, `BK_CI_BUILD_TASK_ID`, `BK_CI_ATOM_CODE` | 当前执行位置 |
| **Git/Webhook** | `BK_CI_GIT_REPO_URL`, `BK_REPO_GIT_WEBHOOK_BRANCH`, `BK_CI_GIT_MR_TITLE` | 代码仓库信息 |
| **带前缀变量** | `BK_REPO_GIT_WEBHOOK_PUSH_COMMIT_MSG_1`, `BK_REPO_GIT_WEBHOOK_PUSH_ADD_FILE_1` | 支持通配的变量 |

> 完整变量列表请参考：`api-process/.../enums/VariableType.kt`  
> 表达式语法映射（`ci.xxx`）请参考 6.2 节

### 3.3 触发参数解析（parseTriggerParam）

在构建启动时，用户通过前端手动输入或 API 传入的参数需要与流水线定义的参数进行合并和校验。这个过程由 `BuildParametersCompatibilityTransformer.parseTriggerParam()` 方法完成。

```kotlin
// 文件: biz-base/.../engine/compatibility/v2/V2BuildParametersCompatibilityTransformer.kt

class V2BuildParametersCompatibilityTransformer : BuildParametersCompatibilityTransformer {

    /**
     * 解析前端手工启动传入的参数并与 TriggerContainer 的 BuildFormProperty 合并
     * 
     * @param userId 操作用户 ID
     * @param projectId 项目 ID
     * @param pipelineId 流水线 ID
     * @param paramProperties 流水线定义的参数列表（来自 TriggerContainer.params）
     * @param paramValues 用户传入的参数值（前端/API 传入）
     * @return 合并后的参数映射 Map<String, BuildParameters>
     */
    override fun parseTriggerParam(
        userId: String,
        projectId: String,
        pipelineId: String,
        paramProperties: List<BuildFormProperty>,
        paramValues: Map<String, String>
    ): MutableMap<String, BuildParameters> {
        
        val paramsMap = HashMap<String, BuildParameters>()
        
        paramProperties.forEach { param ->
            // 1. 变量名兼容转换：旧变量名 → 新变量名
            val key = PipelineVarUtil.oldVarToNewVar(param.id) ?: param.id
            
            // 2. 确定最终值
            val value = when {
                // 2.1 常量保护：constant=true 时强制使用默认值
                param.constant == true -> {
                    param.readOnly = true  // 强制设为只读
                    param.defaultValue
                }
                
                // 2.2 自定义文件版本控制处理
                param.type == BuildFormPropertyType.CUSTOM_FILE 
                    && param.enableVersionControl == true -> {
                    // 解析版本控制信息 JSON
                    val versionControlInfo = paramValues[key]?.let { str ->
                        JsonUtil.to(str, CustomFileVersionControlInfo::class.java)
                    }
                    versionControlInfo?.directory ?: param.defaultValue
                }
                
                // 2.3 普通参数：用户传入值覆盖默认值
                else -> {
                    paramValues[key] ?: paramValues[param.id] ?: param.defaultValue
                }
            }
            
            // 3. 空值校验（valueNotEmpty=true 时）
            if (param.valueNotEmpty == true && value.toString().isEmpty()) {
                // 检查是否因条件不满足而隐藏
                val isHidden = param.displayCondition?.any { (condKey, condValue) ->
                    paramValues[condKey] != condValue
                } ?: false
                
                // 参数未被隐藏时，抛出异常
                if (!isHidden) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_BUILD_START_PARAM_NO_EMPTY,
                        params = arrayOf(param.id)
                    )
                }
            }
            
            // 4. 构建参数对象
            paramsMap[key] = BuildParameters(
                key = key,
                value = value,
                valueType = param.type,
                readOnly = param.readOnly,
                desc = param.desc,
                defaultValue = param.defaultValue
            )
        }
        
        return paramsMap
    }
}
```

**核心处理逻辑**

| 处理步骤 | 说明 |
|----------|------|
| **变量名兼容** | 通过 `PipelineVarUtil.oldVarToNewVar()` 将旧变量名（如 `pipeline.id`）转换为新变量名（如 `BK_CI_PIPELINE_ID`） |
| **常量保护** | 如果参数定义为 `constant=true`，忽略用户传入值，强制使用默认值并设为只读 |
| **自定义文件** | 对于 `CUSTOM_FILE` 类型且启用版本控制的参数，解析 JSON 格式的版本控制信息 |
| **值覆盖** | 用户传入值优先级高于默认值（常量除外） |
| **空值校验** | 如果 `valueNotEmpty=true` 且值为空，抛出异常（隐藏的参数跳过校验） |
| **条件显示** | 根据 `displayCondition` 判断参数是否被隐藏，隐藏的参数跳过空值校验 |

**调用场景**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        parseTriggerParam 调用场景                            │
└─────────────────────────────────────────────────────────────────────────────┘

1. 手动触发构建
   PipelineBuildFacadeService.buildManualStartup()
       └── buildParamCompatibilityTransformer.parseTriggerParam(...)

2. API 触发构建
   PipelineBuildFacadeService.startPipeline()
       └── buildParamCompatibilityTransformer.parseTriggerParam(...)

3. Webhook 触发构建
   PipelineBuildWebhookService.webhookTrigger()
       └── buildParamCompatibilityTransformer.parseTriggerParam(...)

4. 子流水线调用
   SubPipelineStartUpService.callPipelineStartup()
       └── buildParamCompatibilityTransformer.parseTriggerParam(...)
```

**源码位置**

| 类名 | 文件路径 |
|------|----------|
| `BuildParametersCompatibilityTransformer` | `biz-base/.../engine/compatibility/BuildParametersCompatibilityTransformer.kt` |
| `V2BuildParametersCompatibilityTransformer` | `biz-base/.../engine/compatibility/v2/V2BuildParametersCompatibilityTransformer.kt` |
| `CompatibilityConfig` | `biz-base/.../engine/compatibility/CompatibilityConfig.kt` |

### 3.4 变量初始化核心方法

变量初始化的核心逻辑在 `PipelineBuildService.initPipelineParamMap()` 方法中，该方法负责填充系统预置变量到 `pipelineParamMap`。

```kotlin
// 文件: biz-base/.../service/pipeline/PipelineBuildService.kt

class PipelineBuildService {
    
    /**
     * 初始化流水线参数映射
     * 填充系统预置变量到 pipelineParamMap
     */
    private fun initPipelineParamMap(
        buildId: String,
        startType: StartType,
        pipelineParamMap: MutableMap<String, BuildParameters>,
        userId: String,
        startValues: Map<String, String>?,
        pipeline: PipelineInfo,
        projectVO: ProjectVO?,
        channelCode: ChannelCode,
        isMobile: Boolean,
        debug: Boolean? = false,
        pipelineAuthorizer: String? = null,
        pipelineDialectType: String,
        failIfVariableInvalid: Boolean? = false
    ) {
        // 1. 触发用户信息
        val userName = when (startType) {
            StartType.PIPELINE -> pipelineParamMap[PIPELINE_START_PIPELINE_USER_ID]?.value
            StartType.WEB_HOOK -> pipelineParamMap[PIPELINE_START_WEBHOOK_USER_ID]?.value
            StartType.SERVICE -> pipelineParamMap[PIPELINE_START_SERVICE_USER_ID]?.value
            StartType.MANUAL -> pipelineParamMap[PIPELINE_START_MANUAL_USER_ID]?.value
            StartType.TIME_TRIGGER -> pipelineParamMap[PIPELINE_START_TIME_TRIGGER_USER_ID]?.value
            StartType.REMOTE -> startValues?.get(PIPELINE_START_REMOTE_USER_ID)
        } ?: userId
        
        pipelineParamMap[PIPELINE_START_USER_ID] = BuildParameters(
            key = PIPELINE_START_USER_ID, value = userId
        )
        pipelineParamMap[PIPELINE_START_USER_NAME] = BuildParameters(
            key = PIPELINE_START_USER_NAME, value = userName
        )
        
        // 2. 流水线基础信息
        pipelineParamMap[PIPELINE_NAME] = BuildParameters(
            key = PIPELINE_NAME,
            value = startValues?.get(PIPELINE_NAME) ?: pipeline.pipelineName
        )
        pipelineParamMap[PROJECT_NAME_CHINESE] = BuildParameters(
            key = PROJECT_NAME_CHINESE,
            value = projectVO?.projectName ?: "",
            valueType = BuildFormPropertyType.STRING
        )
        pipelineParamMap[PIPELINE_ID] = BuildParameters(
            PIPELINE_ID, pipeline.pipelineId, readOnly = true
        )
        pipelineParamMap[PROJECT_NAME] = BuildParameters(
            PROJECT_NAME, pipeline.projectId, readOnly = true
        )
        pipelineParamMap[PIPELINE_VERSION] = BuildParameters(
            PIPELINE_VERSION, pipeline.version, readOnly = true
        )
        
        // 3. 构建信息
        pipelineParamMap[PIPELINE_BUILD_ID] = BuildParameters(
            PIPELINE_BUILD_ID, buildId, readOnly = true
        )
        pipelineParamMap[PIPELINE_BUILD_URL] = BuildParameters(
            key = PIPELINE_BUILD_URL,
            value = pipelineUrlBean.genBuildDetailUrl(
                projectCode = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                buildId = buildId,
                position = null,
                stageId = null,
                needShortUrl = false
            ),
            readOnly = true
        )
        pipelineParamMap[PIPELINE_BUILD_MSG] = BuildParameters(
            key = PIPELINE_BUILD_MSG,
            value = BuildMsgUtils.getBuildMsg(
                buildMsg = startValues?.get(PIPELINE_BUILD_MSG)
                    ?: pipelineParamMap[PIPELINE_BUILD_MSG]?.value?.toString(),
                startType = startType,
                channelCode = channelCode
            ),
            readOnly = true
        )
        
        // 4. 触发信息
        pipelineParamMap[PIPELINE_START_TYPE] = BuildParameters(
            key = PIPELINE_START_TYPE, value = startType.name, readOnly = true
        )
        pipelineParamMap[PIPELINE_START_CHANNEL] = BuildParameters(
            key = PIPELINE_START_CHANNEL, value = channelCode.name, readOnly = true
        )
        pipelineParamMap[PIPELINE_START_MOBILE] = BuildParameters(
            key = PIPELINE_START_MOBILE, value = isMobile, readOnly = true
        )
        
        // 5. 流水线创建/更新用户
        pipelineParamMap[PIPELINE_CREATE_USER] = BuildParameters(
            key = PIPELINE_CREATE_USER, value = pipeline.creator, readOnly = true
        )
        pipelineParamMap[PIPELINE_UPDATE_USER] = BuildParameters(
            key = PIPELINE_UPDATE_USER, value = pipeline.lastModifyUser, readOnly = true
        )
        
        // 6. 调试模式标识
        pipelineParamMap[PIPELINE_BUILD_DEBUG] = BuildParameters(
            PIPELINE_BUILD_DEBUG, debug ?: false, readOnly = true
        )
        
        // 7. 流水线语法类型
        pipelineParamMap[PIPELINE_DIALECT] = BuildParameters(
            PIPELINE_DIALECT, pipelineDialectType, readOnly = true
        )
        
        // 8. 自定义触发材料（可选）
        startValues?.get(BK_CI_MATERIAL_ID)?.let {
            pipelineParamMap[BK_CI_MATERIAL_ID] = BuildParameters(
                key = BK_CI_MATERIAL_ID, value = it, readOnly = true
            )
        }
        
        // 9. 流水线权限代持人（可选）
        pipelineAuthorizer?.let {
            pipelineParamMap[BK_CI_AUTHORIZER] = BuildParameters(
                key = BK_CI_AUTHORIZER, value = it, readOnly = true
            )
        }
        
        // 10. 链路追踪信息
        val bizId = MDC.get(TraceTag.BIZID)
        if (!bizId.isNullOrBlank()) {
            pipelineParamMap[TraceTag.TRACE_HEADER_DEVOPS_BIZID] = BuildParameters(
                key = TraceTag.TRACE_HEADER_DEVOPS_BIZID, value = bizId
            )
        }
    }
}
```

### 3.5 构建启动上下文

`StartBuildContext` 是一个数据类，封装了构建启动所需的所有上下文信息。它的 `init` 方法接收已经初始化好的 `pipelineParamMap`，并生成最终的构建参数列表。

```kotlin
// 文件: api-process/.../pojo/app/StartBuildContext.kt

data class StartBuildContext(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val resourceVersion: Int,
    val actionType: ActionType,           // START 或 RETRY
    val executeCount: Int,                 // 执行次数
    val userId: String,                    // 操作用户
    val triggerUser: String,               // 触发用户
    val startType: StartType,              // 触发类型
    val parentBuildId: String?,            // 父流水线构建 ID
    val parentTaskId: String?,             // 父流水线任务 ID
    val channelCode: ChannelCode,          // 渠道
    val variables: Map<String, String>,    // 变量映射（最终结果）
    val pipelineParamMap: MutableMap<String, BuildParameters>,  // 参数映射
    val buildParameters: MutableList<BuildParameters>,          // 构建参数列表
    // ...
) {
    companion object {
        /**
         * 初始化构建上下文
         * 注意: pipelineParamMap 在调用前已经被 initPipelineParamMap() 填充
         */
        fun init(
            projectId: String,
            pipelineId: String,
            buildId: String,
            resourceVersion: Int,
            modelStr: String,
            pipelineSetting: PipelineSetting?,
            currentBuildNo: Int?,
            triggerReviewers: List<String>?,
            pipelineParamMap: MutableMap<String, BuildParameters>,  // 已初始化
            webHookStartParam: MutableMap<String, BuildParameters>,
            realStartParamKeys: List<String>,  // 用户定义的参数 Key 列表
            debug: Boolean,
            versionName: String?,
            yamlVersion: String?
        ): StartBuildContext {
            // 1. 生成构建参数列表（用于写入 T_PIPELINE_BUILD_VAR）
            val buildParam = genOriginStartParamsList(realStartParamKeys, pipelineParamMap)
            
            // 2. 将 pipelineParamMap 转换为 Map<String, String>
            val params: Map<String, String> = pipelineParamMap.values
                .associate { it.key to it.value.toString() }
            
            // 3. 解析重试相关参数
            val retryStartTaskId = params[PIPELINE_RETRY_START_TASK_ID]
            val (actionType, executeCount, isStageRetry) = if (params[PIPELINE_RETRY_COUNT] != null) {
                Triple(ActionType.RETRY, retryCount, isStageRetry)
            } else {
                Triple(ActionType.START, 1, false)
            }
            
            // 4. 构建上下文对象
            return StartBuildContext(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = params,  // 最终变量映射
                buildParameters = buildParam,  // 构建参数列表
                pipelineParamMap = pipelineParamMap,
                actionType = actionType,
                executeCount = executeCount,
                userId = params[PIPELINE_START_USER_ID]!!,
                triggerUser = params[PIPELINE_START_USER_NAME]!!,
                startType = StartType.valueOf(params[PIPELINE_START_TYPE]!!),
                parentBuildId = params[PIPELINE_START_PARENT_BUILD_ID],
                parentTaskId = params[PIPELINE_START_PARENT_BUILD_TASK_ID],
                channelCode = ChannelCode.valueOf(params[PIPELINE_START_CHANNEL] ?: "BS"),
                // ...
            )
        }
        
        /**
         * 生成构建参数列表
         * 根据用户定义的参数 Key 从 pipelineParamMap 中提取参数
         * 并为每个参数添加 variables. 前缀版本
         */
        private fun genOriginStartParamsList(
            realStartParamKeys: List<String>,
            pipelineParamMap: MutableMap<String, BuildParameters>
        ): ArrayList<BuildParameters> {
            val originStartParams = ArrayList<BuildParameters>()
            val originStartContexts = HashMap<String, BuildParameters>()
            
            realStartParamKeys.forEach { key ->
                pipelineParamMap[key]?.let { param ->
                    originStartParams.add(param)
                    // 添加 variables.xxx 前缀版本
                    fillContextPrefix(param, originStartContexts)
                }
            }
            
            // 将 variables.xxx 版本也加入 pipelineParamMap
            pipelineParamMap.putAll(originStartContexts)
            
            // 添加特殊参数
            pipelineParamMap[BUILD_NO]?.let { originStartParams.add(it) }
            pipelineParamMap[PIPELINE_BUILD_MSG]?.let { originStartParams.add(it) }
            pipelineParamMap[PIPELINE_RETRY_COUNT]?.let { originStartParams.add(it) }
            
            return originStartParams
        }
        
        /**
         * 为参数添加 variables. 前缀
         * MY_VAR → variables.MY_VAR
         */
        private fun fillContextPrefix(
            param: BuildParameters,
            originStartContexts: HashMap<String, BuildParameters>
        ) {
            val key = param.key
            if (key.startsWith("variables.")) {
                originStartContexts[key] = param
            } else {
                val ctxKey = "variables.$key"
                originStartContexts[ctxKey] = param.copy(key = ctxKey)
            }
        }
    }
}
```

### 3.6 变量初始化流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ PipelineBuildFacadeService.buildManualStartup() / startPipeline()            │
│ 源码: biz-process/.../service/builds/PipelineBuildFacadeService.kt          │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 0. 解析触发参数
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ buildParamCompatibilityTransformer.parseTriggerParam()                       │
│ 源码: biz-base/.../engine/compatibility/v2/V2BuildParametersCompatibilityTransformer.kt │
│                                                                              │
│ 处理用户传入参数:                                                            │
│ ├── 变量名兼容转换（旧变量名 → 新变量名）                                   │
│ ├── 常量保护（constant=true 时强制使用默认值）                              │
│ ├── 自定义文件版本控制处理                                                  │
│ ├── 用户传入值覆盖默认值                                                    │
│ └── 空值校验（valueNotEmpty=true）                                          │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 1. pipelineParamMap 已包含用户启动参数和流水线定义参数
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PipelineBuildService.startPipeline()                                         │
│ 源码: biz-base/.../service/pipeline/PipelineBuildService.kt:127             │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 2. 填充系统预置变量
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ initPipelineParamMap()                                                       │
│ 源码: biz-base/.../service/pipeline/PipelineBuildService.kt:291             │
│                                                                              │
│ 填充系统预置变量到 pipelineParamMap:                                         │
│ ├── PIPELINE_START_USER_ID, PIPELINE_START_USER_NAME (触发用户)             │
│ ├── PIPELINE_NAME, PROJECT_NAME, PROJECT_NAME_CHINESE (项目/流水线信息)     │
│ ├── PIPELINE_BUILD_ID, PIPELINE_BUILD_URL (构建信息)                        │
│ ├── PIPELINE_START_TYPE, PIPELINE_START_CHANNEL (触发信息)                  │
│ ├── PIPELINE_VERSION, PIPELINE_CREATE_USER, PIPELINE_UPDATE_USER            │
│ └── 其他系统变量...                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 3. 所有变量已填充到 pipelineParamMap
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ StartBuildContext.init()                                                     │
│ 源码: api-process/.../pojo/app/StartBuildContext.kt:276                     │
│                                                                              │
│ 生成构建上下文:                                                              │
│ ├── genOriginStartParamsList() → 生成 buildParameters 列表                  │
│ ├── 为用户变量添加 variables.xxx 前缀版本                                   │
│ └── 解析重试相关参数 (actionType, executeCount)                             │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 4. 执行拦截器链检查
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ pipelineInterceptorChain.filter()                                            │
│                                                                              │
│ 拦截器检查:                                                                  │
│ ├── 排队检查                                                                 │
│ ├── 并发组检查                                                               │
│ ├── 配额检查                                                                 │
│ └── 其他前置检查...                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 5. 检查通过，开始创建构建
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PipelineRuntimeService.startBuild()                                          │
│ 源码: biz-base/.../engine/service/PipelineRuntimeService.kt                 │
│                                                                              │
│ 创建构建记录:                                                                │
│ ├── T_PIPELINE_BUILD_HISTORY (构建历史)                                      │
│ ├── T_PIPELINE_BUILD_STAGE (Stage 状态)                                      │
│ ├── T_PIPELINE_BUILD_CONTAINER (Job 状态)                                    │
│ └── T_PIPELINE_BUILD_TASK (Task 状态)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 6. 批量保存变量
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ buildVariableService.startBuildBatchSaveWithoutThreadSafety()                │
│ 源码: biz-base/.../service/BuildVariableService.kt                          │
│                                                                              │
│ 参数: context.buildParameters (List<BuildParameters>)                        │
│ 说明: 构建启动时无并发，不需要加锁                                           │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 7. 写入数据库
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PipelineBuildVarDao.batchSave()                                              │
│ 源码: biz-base/.../engine/dao/PipelineBuildVarDao.kt                        │
│                                                                              │
│ INSERT INTO T_PIPELINE_BUILD_VAR                                             │
│ (BUILD_ID, KEY, VALUE, PROJECT_ID, PIPELINE_ID, VAR_TYPE, READ_ONLY)        │
│ VALUES (?, ?, ?, ?, ?, ?, ?)                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 变量动态更新

### 4.1 插件输出变量机制

插件通过 `output.json` 文件输出变量，Worker 读取后上报到引擎。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Worker 端 - 插件执行                                                         │
│ 源码: worker-common/.../task/market/MarketAtomTask.kt                       │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 1. 执行插件
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 插件进程                                                                     │
│                                                                              │
│ 插件通过 SDK 输出变量:                                                       │
│ ├── Python: self.set_output("key", "value")                                 │
│ ├── Java: atomContext.setOutput("key", "value")                             │
│ └── NodeJS: atomContext.setOutput("key", "value")                           │
│                                                                              │
│ 最终写入: {workspace}/.atomOutput/output.json                                │
│ {                                                                            │
│     "status": "success",                                                     │
│     "data": {                                                                │
│         "outputs": {                                                         │
│             "key1": "value1",                                                │
│             "key2": "value2"                                                 │
│         }                                                                    │
│     }                                                                        │
│ }                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 2. 读取输出
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ MarketAtomTask.output()                                                      │
│                                                                              │
│ fun output(atomResult: AtomResult): Map<String, String> {                    │
│     val outputs = mutableMapOf<String, String>()                             │
│     atomResult.data?.forEach { (key, value) ->                               │
│         if (key == "outputs") {                                              │
│             (value as? Map<*, *>)?.forEach { (k, v) ->                       │
│                 outputs[k.toString()] = v.toString()                         │
│             }                                                                │
│         }                                                                    │
│     }                                                                        │
│     return outputs                                                           │
│ }                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 3. 上报结果
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ EngineService.completeTask(buildResult)                                      │
│                                                                              │
│ buildResult 包含:                                                            │
│ ├── status: 任务状态                                                         │
│ ├── message: 错误信息                                                        │
│ └── buildResult: Map<String, String> 输出变量                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 引擎端变量更新

```kotlin
// 文件: biz-base/.../engine/service/vmbuild/EngineVMBuildService.kt

class EngineVMBuildService {
    
    fun buildCompleteTask(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        result: BuildTaskResult
    ) {
        // ...
        
        // 更新变量
        executeCompleteTaskBus(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            result = result,
            buildInfo = buildInfo
        )
    }
    
    private fun executeCompleteTaskBus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        result: BuildTaskResult,
        buildInfo: BuildInfo
    ) {
        // 将插件输出变量写入数据库
        if (result.buildResult.isNotEmpty()) {
            val variables = result.buildResult.map { (key, value) ->
                BuildParameters(
                    key = key,
                    value = value,
                    valueType = BuildFormPropertyType.STRING,
                    readOnly = false
                )
            }
            
            // 批量更新变量
            buildVariableService.batchUpdateVariable(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = variables
            )
        }
        
        // 处理特殊变量: BK_CI_BUILD_REMARK
        writeRemark(projectId, pipelineId, buildId, result)
    }
}
```

### 4.3 变量更新流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ BuildVariableService.batchUpdateVariable()                                   │
│ 源码: biz-base/.../service/BuildVariableService.kt                          │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 1. 获取分布式锁
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PipelineBuildVarLock(redisOperation, buildId).use { lock ->                  │
│     lock.lock()  // 获取锁，防止并发冲突                                     │
│     // ...                                                                   │
│ }                                                                            │
│                                                                              │
│ 锁 Key: pipelineBuildVar:{buildId}                                           │
│ 过期时间: 10 秒                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 2. 查询现有变量
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ val existingVars = pipelineBuildVarDao.getVarsWithType(                      │
│     dslContext, projectId, buildId                                           │
│ )                                                                            │
│                                                                              │
│ 构建 existingMap: Map<String, BuildParameters>                               │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 3. 区分新增和更新
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ val toInsert = mutableListOf<BuildParameters>()                              │
│ val toUpdate = mutableListOf<BuildParameters>()                              │
│                                                                              │
│ variables.forEach { variable ->                                              │
│     val existing = existingMap[variable.key]                                 │
│     if (existing == null) {                                                  │
│         toInsert.add(variable)  // 新变量                                    │
│     } else if (!existing.readOnly) {                                         │
│         toUpdate.add(variable)  // 已存在且非只读，更新                      │
│     }                                                                        │
│     // 只读变量跳过                                                          │
│ }                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 4. 执行数据库操作
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ // 批量插入新变量                                                            │
│ if (toInsert.isNotEmpty()) {                                                 │
│     pipelineBuildVarDao.batchSave(dslContext, projectId, pipelineId,         │
│         buildId, toInsert)                                                   │
│ }                                                                            │
│                                                                              │
│ // 批量更新已有变量                                                          │
│ if (toUpdate.isNotEmpty()) {                                                 │
│     pipelineBuildVarDao.batchUpdate(dslContext, projectId, pipelineId,       │
│         buildId, toUpdate)                                                   │
│ }                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 变量传递策略

### 5.1 Job 间变量传递

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Job 间变量传递机制                                  │
└─────────────────────────────────────────────────────────────────────────────┘

Stage-1
├── Job-A (jobId: job_a)
│   ├── Step-1 (stepId: step_1)
│   │   └── 输出: key1 = "value1"
│   └── Step-2 (stepId: step_2)
│       └── 输出: key2 = "value2"
│
└── Job-B (jobId: job_b)
    └── Step-3
        └── 读取: ${{ jobs.job_a.steps.step_1.outputs.key1 }}
                  ${{ jobs.job_a.steps.step_2.outputs.key2 }}

Stage-2
└── Job-C
    └── Step-4
        └── 读取: ${{ jobs.job_a.steps.step_1.outputs.key1 }}
                  (可以跨 Stage 读取)
```

### 5.2 上下文服务

```kotlin
// 文件: biz-base/.../service/PipelineContextService.kt

@Service
class PipelineContextService {
    
    /**
     * 构建完整的执行上下文
     * 包含所有 Job 和 Step 的输出变量
     */
    fun getAllBuildContext(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String? = null,
        taskId: String? = null
    ): Map<String, String> {
        val context = mutableMapOf<String, String>()
        
        // 1. 获取所有变量
        val allVars = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        context.putAll(allVars)
        
        // 2. 构建 Job 上下文
        buildJobContext(projectId, pipelineId, buildId, context)
        
        // 3. 构建 Step 上下文（当前 Job 内）
        if (containerId != null) {
            buildStepContext(projectId, pipelineId, buildId, containerId, context)
        }
        
        // 4. 填充 CI 预置变量
        PipelineVarUtil.fillContextVarMap(context)
        
        return context
    }
    
    /**
     * 构建 Job 上下文
     * 格式: jobs.<jobId>.steps.<stepId>.outputs.<key>
     */
    private fun buildJobContext(
        projectId: String,
        pipelineId: String,
        buildId: String,
        context: MutableMap<String, String>
    ) {
        // 查询所有已完成的 Task
        val tasks = pipelineBuildTaskDao.listByBuildId(dslContext, projectId, buildId)
        
        tasks.forEach { task ->
            val jobId = task.containerId
            val stepId = task.taskId
            
            // 获取 Task 输出变量
            val outputs = getTaskOutputs(projectId, buildId, stepId)
            
            outputs.forEach { (key, value) ->
                // 格式: jobs.job_a.steps.step_1.outputs.key1
                context["jobs.$jobId.steps.$stepId.outputs.$key"] = value
            }
        }
    }
    
    /**
     * 构建 Step 上下文（当前 Job 内）
     * 格式: steps.<stepId>.outputs.<key>
     */
    private fun buildStepContext(
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String,
        context: MutableMap<String, String>
    ) {
        // 查询当前 Job 的所有 Task
        val tasks = pipelineBuildTaskDao.listByContainerId(
            dslContext, projectId, buildId, containerId
        )
        
        tasks.forEach { task ->
            val stepId = task.taskId
            val outputs = getTaskOutputs(projectId, buildId, stepId)
            
            outputs.forEach { (key, value) ->
                // 格式: steps.step_1.outputs.key1
                context["steps.$stepId.outputs.$key"] = value
            }
        }
    }
}
```

### 5.3 父子流水线变量传递

```kotlin
// 文件: biz-process/.../service/SubPipelineStartUpService.kt

@Service
class SubPipelineStartUpService {
    
    /**
     * 调用子流水线
     */
    fun callPipelineStartup(
        projectId: String,
        parentPipelineId: String,
        parentBuildId: String,
        parentTaskId: String,
        subPipelineId: String,
        params: Map<String, String>,
        channelCode: ChannelCode
    ): BuildId {
        // 1. 构建传递给子流水线的参数
        val subParams = mutableMapOf<String, String>()
        
        // 2. 添加父流水线信息（系统变量）
        subParams[PIPELINE_START_PARENT_PROJECT_ID] = projectId
        subParams[PIPELINE_START_PARENT_PIPELINE_ID] = parentPipelineId
        subParams[PIPELINE_START_PARENT_BUILD_ID] = parentBuildId
        subParams[PIPELINE_START_PARENT_BUILD_NUM] = getParentBuildNum(parentBuildId)
        subParams[PIPELINE_START_PARENT_BUILD_TASK_ID] = parentTaskId
        
        // 3. 添加用户指定的传递参数
        subParams.putAll(params)
        
        // 4. 启动子流水线
        return pipelineBuildService.startPipeline(
            projectId = projectId,
            pipelineId = subPipelineId,
            startParams = subParams,
            startType = StartType.PIPELINE,
            triggerUser = getTriggerUser(parentBuildId),
            channelCode = channelCode
        )
    }
}
```

**父子流水线传递的系统变量**

| 变量名 | 说明 |
|--------|------|
| `BK_CI_PARENT_PROJECT_ID` | 父流水线项目 ID |
| `BK_CI_PARENT_PIPELINE_ID` | 父流水线 ID |
| `BK_CI_PARENT_BUILD_ID` | 父流水线构建 ID |
| `PIPELINE_START_PARENT_BUILD_NUM` | 父流水线构建号 |
| `PIPELINE_START_PARENT_BUILD_TASK_ID` | 父流水线调用任务 ID |

---

## 6. 变量表达式解析

### 6.1 表达式语法

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           表达式语法规范                                      │
└─────────────────────────────────────────────────────────────────────────────┘

基本语法: ${{ <expression> }}

命名空间:
├── variables.xxx      → 流水线变量
├── ci.xxx             → CI 预置变量
├── settings.xxx       → 流水线设置
├── jobs.xxx           → Job 输出
├── steps.xxx          → Step 输出
└── matrix.xxx         → 矩阵变量

示例:
├── ${{ variables.MY_VAR }}
├── ${{ ci.build_id }}
├── ${{ jobs.job_a.steps.step_1.outputs.result }}
├── ${{ steps.step_1.outputs.result }}
└── ${{ matrix.os }}

运算符:
├── 比较: ==, !=, <, >, <=, >=
├── 逻辑: &&, ||, !
├── 字符串: contains(), startsWith(), endsWith()
└── 条件: if-else

条件表达式示例:
├── ${{ variables.ENV == 'prod' }}
├── ${{ ci.build_num > 100 }}
├── ${{ contains(variables.BRANCH, 'release') }}
└── ${{ startsWith(ci.branch, 'feature/') }}
```

### 6.2 CI 预置变量映射（ci.xxx ↔ 数据库变量）

数据库 `T_PIPELINE_BUILD_VAR` 中存储的是 `BK_CI_*` 格式的变量名，而表达式中使用的是 `ci.xxx` 命名空间。两者之间通过 `PipelineVarUtil.contextVarMappingBuildVar` 进行映射转换。

**常用映射表**

| 分类 | 表达式 `ci.xxx` | 数据库变量 `BK_CI_*` |
|------|-----------------|---------------------|
| **构建信息** | `ci.build_id` | `BK_CI_BUILD_ID` |
| | `ci.build_num` | `BK_CI_BUILD_NUM` |
| | `ci.build_url` | `BK_CI_BUILD_URL` |
| **流水线** | `ci.pipeline_id` | `BK_CI_PIPELINE_ID` |
| | `ci.pipeline_name` | `BK_CI_PIPELINE_NAME` |
| **项目** | `ci.project_id` | `BK_CI_PROJECT_NAME` |
| | `ci.project_name` | `BK_CI_PROJECT_NAME_CN` |
| **触发信息** | `ci.actor` | `BK_CI_START_USER_NAME` |
| | `ci.build_start_type` | `BK_CI_START_TYPE` |
| **Git** | `ci.branch` | `BK_REPO_GIT_WEBHOOK_BRANCH` |
| | `ci.sha` | `BK_CI_GIT_SHA` |
| | `ci.ref` | `BK_CI_GIT_REF` |
| | `ci.repo_url` | `BK_CI_GIT_REPO_URL` |
| **MR/PR** | `ci.mr_id` | `BK_CI_GIT_MR_ID` |
| | `ci.mr_title` | `BK_CI_GIT_MR_TITLE` |
| | `ci.head_ref` | `BK_CI_GIT_HEAD_REF` (源分支) |
| | `ci.base_ref` | `BK_CI_GIT_BASE_REF` (目标分支) |

> 完整映射请参考源码：`api-process/.../utils/PipelineVarUtil.kt` 中的 `contextVarMappingBuildVar`

**核心方法**

```kotlin
// 文件: api-process/.../utils/PipelineVarUtil.kt

object PipelineVarUtil {
    
    /** 填充 CI 预置变量到上下文，将 BK_CI_* 转换为 ci.xxx */
    fun fillContextVarMap(varMap: MutableMap<String, String>, buildVar: Map<String, String>)
    
    /** 根据 ci.xxx 获取对应的数据库变量名 */
    fun fetchVarName(contextKey: String): String?
    
    /** 根据数据库变量名获取对应的 ci.xxx */
    fun fetchReverseVarName(varKey: String): String?
}
```

**转换流程**

```
数据库: T_PIPELINE_BUILD_VAR
┌──────────────────────────────────┐
│ KEY = "BK_CI_BUILD_ID"           │
│ VALUE = "b-xxx-xxx"              │
└──────────────────────────────────┘
       │
       │ BuildVariableService.getAllVariable()
       ▼
buildVar: { "BK_CI_BUILD_ID" → "b-xxx-xxx" }
       │
       │ PipelineVarUtil.fillContextVarMap()
       ▼
contextMap: {
    "BK_CI_BUILD_ID" → "b-xxx-xxx",  // 原始变量保留
    "ci.build_id" → "b-xxx-xxx"       // 新增 ci.xxx 映射
}
       │
       ▼
表达式解析器可以同时识别两种写法:
├── ${{ ci.build_id }}        ✅
└── ${{ BK_CI_BUILD_ID }}     ✅
```

**使用两种写法的原因**

| 原因 | 说明 |
|------|------|
| **历史兼容** | 保留 `BK_CI_*` 确保老流水线不受影响 |
| **场景适配** | Shell 用环境变量 `$BK_CI_BUILD_ID`，YAML 用表达式 `${{ ci.build_id }}` |
| **业界对齐** | `ci.xxx` 风格与 GitHub Actions 的 `github.xxx` 一致 |
| **表达式支持** | `${{ }}` 语法支持条件判断如 `${{ ci.build_num > 100 }}` |

### 6.3 表达式解析器

表达式解析涉及三个核心类，它们协同工作完成变量替换：

| 类名 | 文件路径 | 职责 |
|------|----------|------|
| `ExpressionParser` | `common-expression/.../ExpressionParser.kt` | 表达式语法解析和计算 |
| `EnvReplacementParser` | `common-pipeline/.../EnvReplacementParser.kt` | 字符串中的变量替换 |
| `ExprReplacementUtil` | `common-pipeline/.../utils/ExprReplacementUtil.kt` | 表达式替换工具方法 |

**解析流程**

```
输入: "Build ${{ ci.build_num }} on ${{ ci.branch }}"
       │
       ▼
EnvReplacementParser.parse()
       │
       ├── 正则匹配 ${{ ... }}
       │
       ▼
ExpressionParser.evaluateByMap()
       │
       ├── 构建表达式上下文（按命名空间分类）
       ├── 解析表达式树
       └── 计算并返回结果
       │
       ▼
输出: "Build 123 on master"
```

**核心方法**

```kotlin
// ExpressionParser - 表达式计算
fun evaluateByMap(expression: String, variables: Map<String, String>): ExpressionResult

// EnvReplacementParser - 变量替换
fun parse(value: String, contextMap: Map<String, String>): String

// ExprReplacementUtil - 工具方法
fun parseExpression(value: String, contextMap: Map<String, String>): String
```

**支持的语法**

| 语法 | 示例 | 优先级 |
|------|------|--------|
| `${{ }}` 表达式 | `${{ ci.build_num > 100 }}` | 最高 |
| `${}` 变量引用 | `${BK_CI_BUILD_ID}` | 中 |
| `$xxx` 简单变量 | `$BK_CI_BUILD_ID` | 最低 |

---

## 7. 变量工具类

### 7.1 新旧变量名映射

BK-CI 经历了三代变量命名演进，需要兼容处理：

| 代次 | 格式 | 示例 |
|------|------|------|
| 第一代 | 点分隔 | `pipeline.build.id` |
| 第二代 | 环境变量风格 | `BK_CI_BUILD_ID` |
| 第三代 | 表达式命名空间 | `ci.build_id` |

```kotlin
// 文件: api-process/.../utils/PipelineVarUtil.kt

object PipelineVarUtil {
    
    // 旧变量名 → 新变量名映射
    private val oldVarMappingNewVar = mapOf(
        "pipeline.id" to PIPELINE_ID,                    // BK_CI_PIPELINE_ID
        "pipeline.name" to PIPELINE_NAME,                // BK_CI_PIPELINE_NAME
        "pipeline.build.id" to PIPELINE_BUILD_ID,        // BK_CI_BUILD_ID
        "pipeline.build.num" to PIPELINE_BUILD_NUM,      // BK_CI_BUILD_NUM
        "pipeline.start.user.id" to PIPELINE_START_USER_ID,  // BK_CI_START_USER_ID
        "project.name" to PROJECT_NAME,                  // BK_CI_PROJECT_NAME
        "project.name.chinese" to PROJECT_NAME_CHINESE,  // BK_CI_PROJECT_NAME_CN
        // ...
    )
    
    /** 旧变量名转新变量名 */
    fun oldVarToNewVar(oldVarName: String): String?
    
    /** 新变量名转旧变量名 */
    fun newVarToOldVar(newVarName: String): String?
    
    /** 混合新旧变量（兼容处理），同时设置新旧两个变量名 */
    fun mixOldVarAndNewVar(vars: MutableMap<String, String>, varName: String, value: String)
    
    /** 填充 CI 预置变量到上下文（详见 6.2 节） */
    fun fillContextVarMap(varMap: MutableMap<String, String>, buildVar: Map<String, String>)
}
```

---

## 8. 最佳实践

### 8.1 变量命名规范

```
推荐命名规范:

1. 用户自定义变量
   ├── 使用大写字母和下划线: MY_VARIABLE
   ├── 添加业务前缀: DEPLOY_ENV, BUILD_TYPE
   └── 避免与系统变量冲突: 不要使用 BK_CI_ 前缀

2. 插件输出变量
   ├── 使用小写字母: result, version, artifact_path
   └── 添加插件前缀: git_commit_id, docker_image_tag

3. 敏感变量
   ├── 使用凭证管理: 不要直接存储密码
   └── 标记为只读: readOnly = true
```

### 8.2 性能优化建议

```kotlin
// 1. 批量操作优先
// ❌ 不推荐: 循环单个保存
variables.forEach { variable ->
    buildVariableService.setVariable(projectId, pipelineId, buildId, variable.key, variable.value)
}

// ✅ 推荐: 批量保存
buildVariableService.batchUpdateVariable(projectId, pipelineId, buildId, variables)

// 2. 按需查询
// ❌ 不推荐: 获取所有变量
val allVars = buildVariableService.getAllVariable(projectId, pipelineId, buildId)

// ✅ 推荐: 指定需要的变量
val vars = buildVariableService.getAllVariable(
    projectId, pipelineId, buildId,
    keys = setOf("MY_VAR1", "MY_VAR2")
)

// 3. 避免频繁锁竞争
// 插件输出变量尽量一次性输出，减少 completeTask 调用次数
```

### 8.3 调试技巧

```yaml
# 在流水线中打印所有变量
- script: |
    echo "=== 系统变量 ==="
    echo "BUILD_ID: ${{ ci.build_id }}"
    echo "PIPELINE_ID: ${{ ci.pipeline_id }}"
    echo "BUILD_NUM: ${{ ci.build_num }}"
    
    echo "=== 用户变量 ==="
    echo "MY_VAR: ${{ variables.MY_VAR }}"
    
    echo "=== Job 输出 ==="
    echo "Result: ${{ jobs.job_a.steps.step_1.outputs.result }}"
```

---

## 9. 源码位置索引

| 功能 | 类名 | 文件路径 |
|------|------|----------|
| **存储层** | | |
| 变量 DAO | `PipelineBuildVarDao` | `biz-base/.../engine/dao/PipelineBuildVarDao.kt` |
| 变量服务 | `BuildVariableService` | `biz-base/.../service/BuildVariableService.kt` |
| 分布式锁 | `PipelineBuildVarLock` | `biz-base/.../control/lock/PipelineBuildVarLock.kt` |
| **初始化** | | |
| 触发参数解析 | `V2BuildParametersCompatibilityTransformer.parseTriggerParam()` | `biz-base/.../engine/compatibility/v2/V2BuildParametersCompatibilityTransformer.kt` |
| 参数转换器接口 | `BuildParametersCompatibilityTransformer` | `biz-base/.../engine/compatibility/BuildParametersCompatibilityTransformer.kt` |
| 变量初始化 | `PipelineBuildService.initPipelineParamMap()` | `biz-base/.../service/pipeline/PipelineBuildService.kt:291` |
| 构建上下文 | `StartBuildContext` | `api-process/.../pojo/app/StartBuildContext.kt` |
| 运行时服务 | `PipelineRuntimeService` | `biz-base/.../engine/service/PipelineRuntimeService.kt` |
| **动态更新** | | |
| 引擎服务 | `EngineVMBuildService` | `biz-base/.../engine/service/vmbuild/EngineVMBuildService.kt` |
| 插件任务 | `MarketAtomTask` | `worker-common/.../task/market/MarketAtomTask.kt` |
| **传递** | | |
| 上下文服务 | `PipelineContextService` | `biz-base/.../service/PipelineContextService.kt` |
| 子流水线 | `SubPipelineStartUpService` | `biz-process/.../service/SubPipelineStartUpService.kt` |
| **解析** | | |
| 表达式解析 | `ExpressionParser` | `common-expression/.../ExpressionParser.kt` |
| 环境替换 | `EnvReplacementParser` | `common-pipeline/.../EnvReplacementParser.kt` |
| 替换工具 | `ExprReplacementUtil` | `common-pipeline/.../utils/ExprReplacementUtil.kt` |
| **工具** | | |
| 变量工具 | `PipelineVarUtil` | `api-process/.../utils/PipelineVarUtil.kt` |
| 变量类型 | `VariableType` | `api-process/.../enums/VariableType.kt` |
| 常量定义 | `Constants` | `api-process/.../utils/Constants.kt` |

---

## 10. 相关 Skill

- `00-bkci-global-architecture` - 全局架构指南
- `26-pipeline-variable-extension` - 流水线变量字段扩展指南
- `29-process-module-architecture` - Process 模块架构总览
- `42-worker-module-architecture` - Worker 执行器模块架构
