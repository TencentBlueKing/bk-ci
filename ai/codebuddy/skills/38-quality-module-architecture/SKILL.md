# Quality 质量红线模块架构指南

## 概述

Quality（质量红线）模块是 BK-CI 的质量管控服务，负责在流水线执行过程中进行质量检查和拦截，确保只有符合质量标准的构建才能继续执行或发布。

**模块职责**：
- 质量规则管理（创建、编辑、启用/禁用）
- 质量指标管理（内置指标、自定义指标）
- 控制点管理（准入/准出检查点）
- 质量红线拦截与放行
- 质量数据统计与报表

## 一、模块结构

```
src/backend/ci/core/quality/
├── api-quality/           # API 接口定义
│   └── src/main/kotlin/com/tencent/devops/quality/
│       ├── api/           # Resource 接口
│       │   ├── UserGroupResource.kt
│       │   ├── v2/        # V2 版本 API
│       │   │   ├── UserQualityRuleResource.kt
│       │   │   ├── ServiceQualityRuleResource.kt
│       │   │   ├── UserQualityIndicatorResource.kt
│       │   │   └── ...
│       │   └── v3/        # V3 版本 API
│       ├── constant/      # 常量定义
│       │   ├── QualityMessageCode.kt
│       │   ├── I18nConstants.kt
│       │   └── MQ.kt
│       └── pojo/          # 数据传输对象
│           ├── QualityRule*.kt
│           ├── Group*.kt
│           └── RuleInterceptHistory.kt
├── biz-quality/           # 业务逻辑层
└── boot-quality/          # 启动模块
```

## 二、核心概念

### 2.1 质量红线模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        质量红线核心模型                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     质量规则 (Quality Rule)                        │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ 规则名称    │  │ 控制点      │  │ 指标阈值列表             │   │   │
│  │  │ (name)      │  │ (control    │  │ (indicator_range)       │   │   │
│  │  │             │  │  point)     │  │                         │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  │                                                                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │                    生效范围                                  │ │   │
│  │  │  pipeline_template_range | indicator_range                 │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     控制点 (Control Point)                        │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ 原子类型    │  │ 研发阶段    │  │ 红线位置                 │   │   │
│  │  │ (element    │  │ (stage)     │  │ (BEFORE/AFTER)          │   │   │
│  │  │  type)      │  │             │  │                         │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     质量指标 (Indicator)                          │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ 指标名称    │  │ 数据类型    │  │ 阈值操作                 │   │   │
│  │  │ (name)      │  │ (data_type) │  │ (>=, <=, =, !=)         │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心实体关系

| 实体 | 说明 | 关系 |
|------|------|------|
| Quality Rule | 质量规则 | 包含多个指标阈值 |
| Control Point | 控制点 | 关联到流水线原子 |
| Indicator | 质量指标 | 定义检查项和阈值 |
| Metadata | 元数据 | 指标的数据来源 |
| History | 拦截历史 | 记录每次检查结果 |

### 2.3 红线位置

```kotlin
enum class ControlPointPosition {
    BEFORE,  // 准入检查（原子执行前）
    AFTER    // 准出检查（原子执行后）
}
```

## 三、数据库设计

### 3.1 核心表结构

| 表名 | 说明 |
|------|------|
| `T_QUALITY_RULE` | 质量规则表 |
| `T_QUALITY_CONTROL_POINT` | 控制点表 |
| `T_QUALITY_INDICATOR` | 质量指标表 |
| `T_QUALITY_METADATA` | 指标元数据表 |
| `T_QUALITY_HIS_DETAIL_METADATA` | 历史元数据详情 |
| `T_HISTORY` | 拦截历史记录 |
| `T_GROUP` | 通知组（红线触发通知） |
| `T_COUNT_INTERCEPT` | 拦截统计（按项目/日期） |
| `T_COUNT_PIPELINE` | 流水线拦截统计 |
| `T_COUNT_RULE` | 规则拦截统计 |
| `T_CONTROL_POINT` | 控制点定义 |
| `T_CONTROL_POINT_METADATA` | 控制点元数据 |
| `T_CONTROL_POINT_TASK` | 控制点任务 |

### 3.2 关键字段说明

**T_QUALITY_RULE**：
```sql
- ID: 规则主键
- NAME: 规则名称
- PROJECT_ID: 项目ID（english_name）
- CONTROL_POINT: 控制点原子类型
- CONTROL_POINT_POSITION: 红线位置（BEFORE/AFTER）
- INDICATOR_RANGE: 指标阈值范围（JSON）
- PIPELINE_TEMPLATE_RANGE: 生效流水线/模板范围
- ENABLE: 是否启用
- GATEWAY_ID: 红线匹配ID
```

**T_QUALITY_INDICATOR**：
```sql
- ID: 指标主键
- ELEMENT_TYPE: 关联的原子类型
- ELEMENT_NAME: 原子名称
- ELEMENT_DETAIL: 原子详情
- EN_NAME: 英文名称
- CN_NAME: 中文名称
- METADATA_IDS: 关联的元数据ID
- DEFAULT_OPERATION: 默认操作符
- OPERATION_AVAILABLE: 可用操作符
- THRESHOLD: 阈值
- THRESHOLD_TYPE: 阈值类型
```

## 四、API 接口设计

### 4.1 用户级接口

```kotlin
@Path("/user/rules")
interface UserQualityRuleResource {
    
    @POST
    @Path("/projects/{projectId}/create")
    fun create(
        @PathParam("projectId") projectId: String,
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        rule: RuleCreate
    ): Result<String>
    
    @GET
    @Path("/projects/{projectId}/list")
    fun list(
        @PathParam("projectId") projectId: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<Page<QualityRule>>
    
    @PUT
    @Path("/projects/{projectId}/{ruleHashId}/update")
    fun update(
        @PathParam("projectId") projectId: String,
        @PathParam("ruleHashId") ruleHashId: String,
        rule: RuleUpdate
    ): Result<Boolean>
    
    @DELETE
    @Path("/projects/{projectId}/{ruleHashId}/delete")
    fun delete(
        @PathParam("projectId") projectId: String,
        @PathParam("ruleHashId") ruleHashId: String
    ): Result<Boolean>
}
```

### 4.2 服务间接口

```kotlin
@Path("/service/rules")
interface ServiceQualityRuleResource {
    
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/check")
    fun check(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        request: QualityCheckRequest
    ): Result<QualityCheckResult>
    
    @GET
    @Path("/projects/{projectId}/matchRuleList")
    fun matchRuleList(
        @PathParam("projectId") projectId: String,
        @QueryParam("pipelineId") pipelineId: String
    ): Result<List<QualityRule>>
}
```

### 4.3 构建过程接口

```kotlin
@Path("/build/quality")
interface BuildQualityMetadataResource {
    
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/metadata/save")
    fun saveMetadata(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        metadata: List<QualityMetadata>
    ): Result<Boolean>
}
```

## 五、质量检查流程

### 5.1 红线检查时序

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ Process │     │ Quality │     │ 指标    │     │ 历史    │
│ Engine  │     │ Service │     │ Service │     │ Service │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.检查红线    │               │               │
     │──────────────>│               │               │
     │               │               │               │
     │               │ 2.获取匹配规则│               │
     │               │──────────────>│               │
     │               │               │               │
     │               │ 3.获取指标数据│               │
     │               │<──────────────│               │
     │               │               │               │
     │               │ 4.比对阈值    │               │
     │               │───────┐       │               │
     │               │       │       │               │
     │               │<──────┘       │               │
     │               │               │               │
     │               │ 5.记录历史    │               │
     │               │──────────────────────────────>│
     │               │               │               │
     │ 6.返回结果    │               │               │
     │<──────────────│               │               │
     │               │               │               │
     │ 7.拦截/放行   │               │               │
     │───────┐       │               │               │
     │       │       │               │               │
     │<──────┘       │               │               │
```

### 5.2 检查结果处理

```kotlin
enum class QualityCheckResult {
    PASS,           // 通过
    FAIL,           // 失败（拦截）
    WAIT,           // 等待审核
    INTERCEPT       // 已拦截
}
```

## 六、与其他模块的关系

### 6.1 依赖关系

```
Quality 模块
    │
    ├──> Process（流水线）
    │    - 在流水线执行时触发质量检查
    │    - 根据检查结果决定是否继续执行
    │
    ├──> Store（研发商店）
    │    - 获取插件的控制点定义
    │    - 获取插件的质量指标元数据
    │
    ├──> Project（项目）
    │    - 获取项目信息
    │    - 项目级别的质量规则管理
    │
    └──> Notify（通知）
         - 红线触发时发送通知
         - 通知审核人员
```

### 6.2 事件机制

```kotlin
// 质量检查事件
data class QualityCheckEvent(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val position: ControlPointPosition,
    val elementType: String
)

// MQ 队列定义
object QualityMQ {
    const val EXCHANGE_QUALITY_CHECK = "e.engine.quality.check"
    const val QUEUE_QUALITY_CHECK = "q.engine.quality.check"
    const val ROUTE_QUALITY_CHECK = "r.engine.quality.check"
}
```

## 七、开发规范

### 7.1 新增质量指标

1. 在 `T_QUALITY_INDICATOR` 表中注册指标
2. 在对应插件中上报元数据
3. 配置指标与控制点的关联

```kotlin
// 插件上报质量数据示例
val metadata = QualityMetadata(
    elementType = "CodeCC",
    data = mapOf(
        "defectCount" to "10",
        "coverageRate" to "85.5"
    )
)
client.get(BuildQualityMetadataResource::class).saveMetadata(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    metadata = listOf(metadata)
)
```

### 7.2 新增控制点

1. 在 `T_QUALITY_CONTROL_POINT` 表中注册
2. 配置支持的红线位置（BEFORE/AFTER）
3. 关联可用的质量指标

### 7.3 自定义规则

```kotlin
// 创建质量规则
val rule = RuleCreate(
    name = "代码扫描质量门禁",
    desc = "代码扫描缺陷数不能超过10个",
    controlPoint = "CodeCC",
    controlPointPosition = "AFTER",
    indicators = listOf(
        RuleIndicator(
            indicatorId = "defectCount",
            operation = "<=",
            threshold = "10"
        )
    ),
    range = listOf("*"),  // 所有流水线
    notifyGroupList = listOf(groupId)
)
```

## 八、常见问题

**Q: 质量规则如何与流水线关联？**
A: 通过 `PIPELINE_TEMPLATE_RANGE` 字段配置生效范围，支持：
- 指定流水线ID列表
- 指定流水线模板
- `*` 表示项目下所有流水线

**Q: 红线拦截后如何处理？**
A: 有三种处理方式：
1. 自动拦截：构建失败
2. 人工审核：等待审核人员决定
3. 告警模式：仅发送通知，不拦截

**Q: 如何查看拦截历史？**
A: 通过 `UserQualityInterceptResource` 接口查询，或在前端"质量红线"页面查看。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
