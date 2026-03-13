# BK-CI 流水线模型数据库 Schema 与持久化

---

## 数据库表

### T_PIPELINE_RESOURCE_VERSION（流水线版本表）

存储每个版本的完整 Model。

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
  `STATUS` varchar(16) DEFAULT NULL COMMENT '版本状态(RELEASED/COMMITTING/BRANCH/DELETE)',
  `BRANCH_ACTION` varchar(32) DEFAULT NULL COMMENT '分支动作',
  `DESCRIPTION` text COMMENT '版本描述',
  `BASE_VERSION` int(11) DEFAULT NULL COMMENT '基础版本（分支用）',
  `REFER_FLAG` bit(1) DEFAULT NULL COMMENT '引用标志',
  `RELEASE_TIME` datetime DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `VERSION`),
  KEY `idx_status` (`STATUS`),
  KEY `idx_release_time` (`RELEASE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### T_PIPELINE_BUILD_RECORD_MODEL（构建记录模型表）

存储构建运行时的 Model 状态快照。

```sql
CREATE TABLE `T_PIPELINE_BUILD_RECORD_MODEL` (
  `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `RESOURCE_VERSION` int(11) NOT NULL COMMENT '关联流水线版本',
  `BUILD_NUM` int(11) DEFAULT NULL COMMENT '构建号',
  `EXECUTE_COUNT` int(11) NOT NULL DEFAULT '1' COMMENT '执行次数（重试递增）',
  `START_USER` varchar(64) DEFAULT NULL COMMENT '启动用户',
  `START_TYPE` varchar(32) DEFAULT NULL COMMENT '启动类型',
  `MODEL_VAR` mediumtext COMMENT '运行时变量和状态JSON',
  `STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
  `START_TIME` datetime DEFAULT NULL,
  `END_TIME` datetime DEFAULT NULL,
  `ERROR_INFO` text COMMENT '错误信息',
  `CANCEL_USER` varchar(64) DEFAULT NULL,
  `TIMESTAMPS` text COMMENT '各阶段时间戳',
  PRIMARY KEY (`BUILD_ID`, `EXECUTE_COUNT`),
  KEY `idx_project_pipeline` (`PROJECT_ID`, `PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## DAO 层

### PipelineResourceVersionDao

```kotlin
@Repository
class PipelineResourceVersionDao {
    // 创建新版本 — 序列化 Model 为 JSON 存入 MODEL 字段
    fun create(dslContext, userId, projectId, pipelineId, version, model: Model, ...): TPipelineResourceVersionRecord?

    // 获取指定版本 Model JSON — 按 version 查询，或取最新 RELEASED 版本
    fun getVersionModelString(dslContext, projectId, pipelineId, version?, includeDraft?): String?
}
```

**文件**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/dao/PipelineResourceVersionDao.kt`

### BuildRecordModelDao

```kotlin
@Repository
class BuildRecordModelDao {
    // 创建构建记录 — 写入运行时状态
    fun createRecord(dslContext, record: BuildRecordModel)

    // 更新构建记录 — 更新 MODEL_VAR、STATUS、时间等
    fun updateRecord(dslContext, projectId, pipelineId, buildId, executeCount, buildStatus?, modelVar, ...)

    // 获取构建记录
    fun getRecord(dslContext, projectId, pipelineId, buildId, executeCount): BuildRecordModel?
}
```

**文件**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/dao/record/BuildRecordModelDao.kt`

---

## Service 层

### PipelineRepositoryService

Model 持久化核心服务。

```kotlin
@Service
class PipelineRepositoryService {
    // 获取流水线 Model
    fun getModel(projectId, pipelineId): Model?

    // 初始化并校验 Model — 调用 ModelCheckPlugin，分配 ID，检查重复
    fun initModel(model, projectId, pipelineId, userId, ...): List<PipelineModelTask>
}
```

initModel 流程:
1. `modelCheckPlugin.checkModelIntegrity(model)` 校验完整性
2. 去重 ID、分配 Stage/Container/Element ID
3. 初始化触发容器和其他容器
4. 检查 Job ID 重复

**文件**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/PipelineRepositoryService.kt`

### PipelineBuildDetailService

构建详情 Model 管理。

```kotlin
@Service
class PipelineBuildDetailService {
    // 更新构建 Model — 提取各层 status 写入 MODEL_VAR
    fun updateModel(projectId, buildId, model)
}
```

**文件**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/PipelineBuildDetailService.kt`

---

## 构建执行引擎 Model 流转

```
构建启动
  → 从 T_PIPELINE_RESOURCE_VERSION 获取 Model
  → 创建 T_PIPELINE_BUILD_RECORD_MODEL 记录
  → 初始化所有 Stage/Container/Element 状态为 QUEUE
  → 逐 Stage 执行: 更新 RUNNING → 调度 Container → 执行 Element
  → 各层状态更新: RUNNING → SUCCEED/FAILED
  → 所有 Stage 完成 → 更新最终状态 → 构建结束
```

**BuildStartControl**: 构建启动时更新触发器状态、Stage 状态、Model 记录

**文件**: `src/backend/ci/core/process/biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/BuildStartControl.kt`

---

## 业务逻辑文件索引

```
src/backend/ci/core/process/
├── biz-base/.../service/
│   ├── record/BaseBuildRecordService.kt          # Model 获取保存
│   ├── record/PipelineBuildRecordService.kt      # 构建记录
│   ├── PipelineBuildDetailService.kt             # 构建详情
│   └── PipelineRepositoryService.kt              # 流水线仓库
├── biz-engine/.../control/
│   ├── BuildStartControl.kt                      # 构建启动
│   └── BuildEndControl.kt                        # 构建结束
└── biz-process/.../service/
    └── PipelineRepositoryService.kt              # 流水线仓库服务
```

---

## Model 与 YAML 转换

Model ↔ YAML 通过 `PipelineTransferYamlService` 实现:
- Model → YAML: 触发器 Stage 转为 `on` 配置，其余 Stage/Container/Element 逐层映射
- YAML → Model: `on` 配置转为 TriggerContainer，stages/jobs/steps 逐层构建

**文件**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/service/pipeline/PipelineTransferYamlService.kt`
