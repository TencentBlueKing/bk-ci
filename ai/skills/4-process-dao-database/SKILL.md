---
name: 4-process-dao-database
description: Process 模块 DAO 层与数据库表结构详细分析，涵盖 JOOQ 使用、表结构设计、索引优化、数据分片。当用户开发 Process 数据访问、设计表结构、优化查询性能或处理数据存储时使用。
---

# Process 模块 DAO 层与数据库表结构详细分析

> **模块路径**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/dao/`
> **数据库**: `devops_process`

## 一、DAO 层概述

### 1.1 目录结构

```
biz-base/src/main/kotlin/com/tencent/devops/process/engine/dao/
├── PipelineInfoDao.kt              # 流水线信息 (29KB)
├── PipelineBuildDao.kt             # 构建历史 (85KB) - 最大
├── PipelineBuildSummaryDao.kt      # 构建摘要 (27KB)
├── PipelineBuildContainerDao.kt    # 构建容器 (14KB)
├── PipelineBuildStageDao.kt        # 构建阶段 (14KB)
├── PipelineBuildTaskDao.kt         # 构建任务 (18KB)
├── PipelineBuildVarDao.kt          # 构建变量 (9KB)
├── PipelineResourceDao.kt          # 流水线资源/模型 (12KB)
├── PipelineResourceVersionDao.kt   # 资源版本 (29KB)
├── PipelineModelTaskDao.kt         # 模型任务索引 (16KB)
├── PipelineOperationLogDao.kt      # 操作日志 (6KB)
├── PipelinePauseValueDao.kt        # 暂停值 (5KB)
├── PipelineWebhookDao.kt           # Webhook (12KB)
├── PipelineWebhookVersionDao.kt    # Webhook 版本 (4KB)
├── PipelineJobMutexGroupDao.kt     # 互斥组 (3KB)
├── PipelineRuleDao.kt              # 规则 (6KB)
├── PipelineTriggerReviewDao.kt     # 触发审核 (3KB)
├── SubPipelineRefDao.kt            # 子流水线引用 (6KB)
├── WebhookBuildParameterDao.kt     # Webhook 参数 (1KB)
└── template/
    ├── TemplateDao.kt              # 模板 (24KB)
    └── TemplatePipelineDao.kt      # 模板流水线 (16KB)
```

### 1.2 DAO 类与表映射

| DAO 类 | 主表 | 职责 |
|--------|------|------|
| `PipelineInfoDao` | `T_PIPELINE_INFO` | 流水线基本信息 |
| `PipelineBuildDao` | `T_PIPELINE_BUILD_HISTORY` | 构建历史记录 |
| `PipelineBuildSummaryDao` | `T_PIPELINE_BUILD_SUMMARY` | 构建摘要统计 |
| `PipelineBuildContainerDao` | `T_PIPELINE_BUILD_CONTAINER` | 构建容器记录 |
| `PipelineBuildStageDao` | `T_PIPELINE_BUILD_STAGE` | 构建阶段记录 |
| `PipelineBuildTaskDao` | `T_PIPELINE_BUILD_TASK` | 构建任务记录 |
| `PipelineBuildVarDao` | `T_PIPELINE_BUILD_VAR` | 构建变量 |
| `PipelineResourceDao` | `T_PIPELINE_RESOURCE` | 流水线模型（JSON） |
| `PipelineResourceVersionDao` | `T_PIPELINE_RESOURCE_VERSION` | 模型版本历史 |
| `PipelineModelTaskDao` | `T_PIPELINE_MODEL_TASK` | 模型任务索引 |
| `TemplateDao` | `T_TEMPLATE` | 流水线模板 |
| `TemplatePipelineDao` | `T_TEMPLATE_PIPELINE` | 模板实例关系 |

## 二、核心数据库表结构

### 2.1 T_PIPELINE_INFO - 流水线信息表

```sql
CREATE TABLE `T_PIPELINE_INFO` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_NAME` varchar(255) NOT NULL COMMENT '流水线名称',
    `PIPELINE_DESC` varchar(255) DEFAULT '' COMMENT '流水线描述',
    `VERSION` int(11) NOT NULL DEFAULT '1' COMMENT '版本号',
    `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
    `LAST_MODIFY_USER` varchar(64) NOT NULL COMMENT '最后修改人',
    `CHANNEL` varchar(32) NOT NULL DEFAULT 'BS' COMMENT '渠道',
    `MANUAL_STARTUP` int(11) DEFAULT '1' COMMENT '是否手动启动',
    `ELEMENT_SKIP` int(11) DEFAULT '0' COMMENT '是否允许跳过插件',
    `TASK_COUNT` int(11) DEFAULT '0' COMMENT '任务数量',
    `DELETE` bit(1) DEFAULT b'0' COMMENT '是否删除',
    `DELETE_TIME` datetime DEFAULT NULL COMMENT '删除时间',
    `LATEST_VERSION_STATUS` varchar(64) DEFAULT 'RELEASED' COMMENT '最新版本状态',
    `LOCKED` bit(1) DEFAULT b'0' COMMENT '是否锁定',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_PIPELINE_ID` (`PIPELINE_ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`),
    KEY `IDX_CHANNEL` (`CHANNEL`),
    KEY `IDX_DELETE` (`DELETE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线信息表';
```

**关键字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `PIPELINE_ID` | varchar(34) | 流水线唯一标识，格式：`p-{uuid}` |
| `PROJECT_ID` | varchar(64) | 所属项目 ID |
| `VERSION` | int | 当前发布版本号 |
| `CHANNEL` | varchar(32) | 渠道：BS(蓝盾)、CODECC、GIT 等 |
| `DELETE` | bit | 软删除标记 |
| `LATEST_VERSION_STATUS` | varchar(64) | RELEASED/COMMITTING/BRANCH_RELEASE |

### 2.2 T_PIPELINE_BUILD_HISTORY - 构建历史表

```sql
CREATE TABLE `T_PIPELINE_BUILD_HISTORY` (
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `PARENT_BUILD_ID` varchar(34) DEFAULT NULL COMMENT '父构建ID',
    `PARENT_TASK_ID` varchar(34) DEFAULT NULL COMMENT '父任务ID',
    `BUILD_NUM` int(11) NOT NULL COMMENT '构建号',
    `TRIGGER` varchar(32) NOT NULL COMMENT '触发方式',
    `STATUS` int(11) NOT NULL COMMENT '构建状态',
    `START_USER` varchar(64) DEFAULT NULL COMMENT '启动用户',
    `TRIGGER_USER` varchar(64) DEFAULT NULL COMMENT '触发用户',
    `START_TIME` datetime DEFAULT NULL COMMENT '开始时间',
    `END_TIME` datetime DEFAULT NULL COMMENT '结束时间',
    `QUEUE_TIME` datetime DEFAULT NULL COMMENT '排队时间',
    `TASK_COUNT` int(11) DEFAULT NULL COMMENT '任务数',
    `FIRST_TASK_ID` varchar(34) DEFAULT NULL COMMENT '第一个任务ID',
    `CHANNEL` varchar(32) DEFAULT 'BS' COMMENT '渠道',
    `VERSION` int(11) DEFAULT NULL COMMENT '流水线版本',
    `EXECUTE_TIME` bigint(20) DEFAULT NULL COMMENT '执行耗时(毫秒)',
    `BUILD_PARAMETERS` mediumtext COMMENT '构建参数(JSON)',
    `WEBHOOK_TYPE` varchar(32) DEFAULT NULL COMMENT 'Webhook类型',
    `RECOMMEND_VERSION` varchar(64) DEFAULT NULL COMMENT '推荐版本号',
    `ERROR_INFO` text COMMENT '错误信息',
    `BUILD_MSG` varchar(255) DEFAULT NULL COMMENT '构建信息',
    `BUILD_NUM_ALIAS` varchar(256) DEFAULT NULL COMMENT '构建号别名',
    `CONCURRENCY_GROUP` varchar(255) DEFAULT NULL COMMENT '并发组',
    `EXECUTE_COUNT` int(11) DEFAULT '1' COMMENT '执行次数',
    `RETRY_FLAG` bit(1) DEFAULT b'0' COMMENT '是否重试',
    `DEBUG_BUILD` bit(1) DEFAULT b'0' COMMENT '是否调试构建',
    PRIMARY KEY (`BUILD_ID`),
    KEY `IDX_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`),
    KEY `IDX_STATUS` (`STATUS`),
    KEY `IDX_START_TIME` (`START_TIME`),
    KEY `IDX_BUILD_NUM` (`BUILD_NUM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建历史表';
```

**关键字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `BUILD_ID` | varchar(34) | 构建唯一标识，格式：`b-{uuid}` |
| `BUILD_NUM` | int | 构建号，自增 |
| `TRIGGER` | varchar(32) | 触发方式：MANUAL/TIME_TRIGGER/WEB_HOOK 等 |
| `STATUS` | int | 构建状态码（见 BuildStatus 枚举） |
| `BUILD_PARAMETERS` | mediumtext | 构建参数 JSON |
| `EXECUTE_COUNT` | int | 执行次数（重试时递增） |

**BuildStatus 状态码**:

| 状态码 | 名称 | 说明 |
|--------|------|------|
| 0 | SUCCEED | 成功 |
| 1 | FAILED | 失败 |
| 2 | CANCELED | 取消 |
| 3 | RUNNING | 运行中 |
| 4 | TERMINATE | 终止 |
| 5 | REVIEWING | 审核中 |
| 6 | REVIEW_ABORT | 审核驳回 |
| 7 | REVIEW_PROCESSED | 审核通过 |
| 8 | HEARTBEAT_TIMEOUT | 心跳超时 |
| 9 | PREPARE_ENV | 准备环境 |
| 10 | UNEXEC | 未执行 |
| 11 | SKIP | 跳过 |
| 12 | QUALITY_CHECK_FAIL | 质量红线失败 |
| 13 | QUEUE | 排队中 |
| 14 | LOOP_WAITING | 循环等待 |
| 15 | CALL_WAITING | 调用等待 |
| 16 | TRY_FINALLY | 执行 finally |
| 17 | QUEUE_TIMEOUT | 排队超时 |
| 18 | EXEC_TIMEOUT | 执行超时 |
| 19 | QUEUE_CACHE | 排队缓存 |
| 20 | RETRY | 重试中 |
| 21 | PAUSE | 暂停 |
| 22 | STAGE_SUCCESS | 阶段成功 |
| 23 | QUOTA_FAILED | 配额失败 |
| 24 | DEPENDENT_WAITING | 依赖等待 |
| 25 | QUALITY_REVIEWING | 质量审核中 |
| 26 | WAIT_QUEUE | 等待排队 |

### 2.3 T_PIPELINE_BUILD_SUMMARY - 构建摘要表

```sql
CREATE TABLE `T_PIPELINE_BUILD_SUMMARY` (
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `BUILD_NUM` int(11) NOT NULL DEFAULT '0' COMMENT '构建号',
    `BUILD_NO` int(11) DEFAULT '0' COMMENT '构建序号',
    `FINISH_COUNT` int(11) DEFAULT '0' COMMENT '完成次数',
    `RUNNING_COUNT` int(11) DEFAULT '0' COMMENT '运行次数',
    `QUEUE_COUNT` int(11) DEFAULT '0' COMMENT '排队次数',
    `LATEST_BUILD_ID` varchar(34) DEFAULT NULL COMMENT '最新构建ID',
    `LATEST_TASK_ID` varchar(34) DEFAULT NULL COMMENT '最新任务ID',
    `LATEST_START_USER` varchar(64) DEFAULT NULL COMMENT '最新启动用户',
    `LATEST_START_TIME` datetime DEFAULT NULL COMMENT '最新开始时间',
    `LATEST_END_TIME` datetime DEFAULT NULL COMMENT '最新结束时间',
    `LATEST_TASK_COUNT` int(11) DEFAULT NULL COMMENT '最新任务数',
    `LATEST_TASK_NAME` varchar(128) DEFAULT NULL COMMENT '最新任务名',
    `LATEST_STATUS` int(11) DEFAULT NULL COMMENT '最新状态',
    `BUILD_NUM_ALIAS` varchar(256) DEFAULT NULL COMMENT '构建号别名',
    `DEBUG_BUILD_NUM` int(11) DEFAULT '0' COMMENT '调试构建号',
    PRIMARY KEY (`PIPELINE_ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建摘要表';
```

### 2.4 T_PIPELINE_BUILD_STAGE - 构建阶段表

```sql
CREATE TABLE `T_PIPELINE_BUILD_STAGE` (
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `STAGE_ID` varchar(34) NOT NULL COMMENT '阶段ID',
    `SEQ` int(11) NOT NULL COMMENT '序号',
    `STATUS` int(11) DEFAULT NULL COMMENT '状态',
    `START_TIME` datetime DEFAULT NULL COMMENT '开始时间',
    `END_TIME` datetime DEFAULT NULL COMMENT '结束时间',
    `COST` int(11) DEFAULT '0' COMMENT '耗时(秒)',
    `EXECUTE_COUNT` int(11) DEFAULT '1' COMMENT '执行次数',
    `CONDITIONS` mediumtext COMMENT '执行条件(JSON)',
    `CHECK_IN` mediumtext COMMENT '准入配置(JSON)',
    `CHECK_OUT` mediumtext COMMENT '准出配置(JSON)',
    PRIMARY KEY (`BUILD_ID`, `STAGE_ID`),
    KEY `IDX_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建阶段表';
```

### 2.5 T_PIPELINE_BUILD_CONTAINER - 构建容器表

```sql
CREATE TABLE `T_PIPELINE_BUILD_CONTAINER` (
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `STAGE_ID` varchar(34) NOT NULL COMMENT '阶段ID',
    `CONTAINER_ID` varchar(34) NOT NULL COMMENT '容器ID',
    `CONTAINER_TYPE` varchar(45) NOT NULL COMMENT '容器类型',
    `SEQ` int(11) NOT NULL COMMENT '序号',
    `STATUS` int(11) DEFAULT NULL COMMENT '状态',
    `START_TIME` datetime DEFAULT NULL COMMENT '开始时间',
    `END_TIME` datetime DEFAULT NULL COMMENT '结束时间',
    `COST` int(11) DEFAULT '0' COMMENT '耗时(秒)',
    `EXECUTE_COUNT` int(11) DEFAULT '1' COMMENT '执行次数',
    `CONDITIONS` mediumtext COMMENT '执行条件(JSON)',
    `CONTAINER_HASH_ID` varchar(64) DEFAULT NULL COMMENT '容器哈希ID',
    `MATRIX_GROUP_FLAG` bit(1) DEFAULT b'0' COMMENT '是否矩阵组',
    `MATRIX_GROUP_ID` varchar(64) DEFAULT NULL COMMENT '矩阵组ID',
    PRIMARY KEY (`BUILD_ID`, `CONTAINER_ID`),
    KEY `IDX_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`),
    KEY `IDX_STAGE_ID` (`STAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建容器表';
```

### 2.6 T_PIPELINE_BUILD_TASK - 构建任务表

```sql
CREATE TABLE `T_PIPELINE_BUILD_TASK` (
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `STAGE_ID` varchar(34) NOT NULL COMMENT '阶段ID',
    `CONTAINER_ID` varchar(34) NOT NULL COMMENT '容器ID',
    `TASK_ID` varchar(34) NOT NULL COMMENT '任务ID',
    `TASK_NAME` varchar(128) DEFAULT NULL COMMENT '任务名称',
    `TASK_TYPE` varchar(64) NOT NULL COMMENT '任务类型',
    `TASK_ATOM` varchar(128) DEFAULT NULL COMMENT '任务插件',
    `ATOM_CODE` varchar(128) DEFAULT NULL COMMENT '插件代码',
    `CLASS_TYPE` varchar(64) DEFAULT NULL COMMENT '类类型',
    `TASK_SEQ` int(11) DEFAULT '1' COMMENT '任务序号',
    `STATUS` int(11) DEFAULT NULL COMMENT '状态',
    `STARTER` varchar(64) DEFAULT NULL COMMENT '启动者',
    `APPROVER` varchar(64) DEFAULT NULL COMMENT '审批者',
    `SUB_PROJECT_ID` varchar(64) DEFAULT NULL COMMENT '子项目ID',
    `SUB_BUILD_ID` varchar(34) DEFAULT NULL COMMENT '子构建ID',
    `START_TIME` datetime DEFAULT NULL COMMENT '开始时间',
    `END_TIME` datetime DEFAULT NULL COMMENT '结束时间',
    `EXECUTE_COUNT` int(11) DEFAULT '0' COMMENT '执行次数',
    `TASK_PARAMS` mediumtext COMMENT '任务参数(JSON)',
    `ADDITIONAL_OPTIONS` mediumtext COMMENT '附加选项(JSON)',
    `TOTAL_TIME` bigint(20) DEFAULT NULL COMMENT '总耗时(毫秒)',
    `ERROR_TYPE` int(11) DEFAULT NULL COMMENT '错误类型',
    `ERROR_CODE` int(11) DEFAULT NULL COMMENT '错误码',
    `ERROR_MSG` text COMMENT '错误信息',
    `CONTAINER_HASH_ID` varchar(64) DEFAULT NULL COMMENT '容器哈希ID',
    `STEP_ID` varchar(64) DEFAULT NULL COMMENT '步骤ID',
    PRIMARY KEY (`BUILD_ID`, `TASK_ID`),
    KEY `IDX_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`),
    KEY `IDX_CONTAINER_ID` (`CONTAINER_ID`),
    KEY `IDX_STATUS` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建任务表';
```

### 2.7 T_PIPELINE_RESOURCE - 流水线资源表

```sql
CREATE TABLE `T_PIPELINE_RESOURCE` (
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `VERSION` int(11) NOT NULL DEFAULT '1' COMMENT '版本',
    `MODEL` mediumtext COMMENT '流水线模型(JSON)',
    `YAML` mediumtext COMMENT 'YAML内容',
    `CREATOR` varchar(64) DEFAULT NULL COMMENT '创建者',
    `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`PIPELINE_ID`, `VERSION`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线资源表';
```

**MODEL 字段示例**:

```json
{
  "name": "示例流水线",
  "desc": "流水线描述",
  "stages": [
    {
      "id": "stage-1",
      "name": "Stage-1",
      "containers": [
        {
          "id": "1",
          "@type": "trigger",
          "name": "构建触发",
          "elements": [
            {
              "id": "T-1-1-1",
              "@type": "manualTrigger",
              "name": "手动触发"
            }
          ]
        }
      ]
    },
    {
      "id": "stage-2",
      "name": "Stage-2",
      "containers": [
        {
          "id": "2",
          "@type": "vmBuild",
          "name": "Linux构建机",
          "baseOS": "LINUX",
          "elements": [
            {
              "id": "T-2-1-1",
              "@type": "linuxScript",
              "name": "Shell脚本",
              "script": "echo hello"
            }
          ]
        }
      ]
    }
  ]
}
```

### 2.8 T_PIPELINE_BUILD_VAR - 构建变量表

```sql
CREATE TABLE `T_PIPELINE_BUILD_VAR` (
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `KEY` varchar(255) NOT NULL COMMENT '变量名',
    `VALUE` mediumtext COMMENT '变量值',
    `PROJECT_ID` varchar(64) DEFAULT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '流水线ID',
    `VAR_TYPE` varchar(64) DEFAULT NULL COMMENT '变量类型',
    `READ_ONLY` bit(1) DEFAULT NULL COMMENT '是否只读',
    PRIMARY KEY (`BUILD_ID`, `KEY`),
    KEY `IDX_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建变量表';
```

### 2.9 T_TEMPLATE - 模板表

```sql
CREATE TABLE `T_TEMPLATE` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `TYPE` varchar(32) NOT NULL DEFAULT 'CUSTOMIZE' COMMENT '模板类型',
    `TEMPLATE_ID` varchar(34) NOT NULL COMMENT '模板ID',
    `TEMPLATE_NAME` varchar(64) NOT NULL COMMENT '模板名称',
    `VERSION_NAME` varchar(64) NOT NULL COMMENT '版本名称',
    `VERSION` bigint(20) NOT NULL DEFAULT '1' COMMENT '版本号',
    `LOGO_URL` varchar(512) DEFAULT NULL COMMENT 'Logo URL',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `SRC_TEMPLATE_ID` varchar(34) DEFAULT NULL COMMENT '源模板ID',
    `DESC` varchar(1024) DEFAULT NULL COMMENT '描述',
    `CATEGORY` varchar(128) DEFAULT NULL COMMENT '分类',
    `CREATOR` varchar(64) NOT NULL COMMENT '创建者',
    `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `TEMPLATE` mediumtext COMMENT '模板内容(JSON)',
    `STORE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否商店模板',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_TEMPLATE_VERSION` (`TEMPLATE_ID`, `VERSION`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线模板表';
```

## 三、核心 DAO 类详解

### 3.1 PipelineInfoDao

**文件**: `PipelineInfoDao.kt` (29KB)

```kotlin
@Repository
class PipelineInfoDao {

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        projectId: String,
        version: Int,
        pipelineName: String,
        pipelineDesc: String,
        userId: String,
        channelCode: ChannelCode,
        manualStartup: Boolean,
        canElementSkip: Boolean,
        taskCount: Int
    ): Int {
        with(T_PIPELINE_INFO) {
            dslContext.insertInto(this,
                PIPELINE_ID, PROJECT_ID, VERSION, PIPELINE_NAME, PIPELINE_DESC,
                CREATE_TIME, UPDATE_TIME, CHANNEL, CREATOR, LAST_MODIFY_USER,
                MANUAL_STARTUP, ELEMENT_SKIP, TASK_COUNT
            ).values(
                pipelineId, projectId, version, pipelineName, pipelineDesc,
                LocalDateTime.now(), LocalDateTime.now(), channelCode.name, userId, userId,
                if (manualStartup) 1 else 0, if (canElementSkip) 1 else 0, taskCount
            ).execute()
        }
        return version
    }

    fun getPipelineInfo(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): TPipelineInfoRecord? {
        return with(T_PIPELINE_INFO) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(DELETE.eq(false))
                .fetchOne()
        }
    }

    fun listPipelineIdByProject(
        dslContext: DSLContext,
        projectId: String,
        channelCode: ChannelCode? = null
    ): List<String> {
        return with(T_PIPELINE_INFO) {
            val query = dslContext.select(PIPELINE_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(DELETE.eq(false))
            channelCode?.let { query.and(CHANNEL.eq(it.name)) }
            query.fetch(PIPELINE_ID)
        }
    }

    fun softDelete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        userId: String
    ): Int {
        return with(T_PIPELINE_INFO) {
            dslContext.update(this)
                .set(DELETE, true)
                .set(DELETE_TIME, LocalDateTime.now())
                .set(LAST_MODIFY_USER, userId)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
```

### 3.2 PipelineBuildDao

**文件**: `PipelineBuildDao.kt` (85KB) - **最大的 DAO 文件**

```kotlin
@Repository
class PipelineBuildDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        version: Int,
        buildNum: Int,
        trigger: String,
        status: BuildStatus,
        startUser: String,
        triggerUser: String,
        buildParameters: String? = null
    ) {
        with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.insertInto(this,
                BUILD_ID, PROJECT_ID, PIPELINE_ID, VERSION, BUILD_NUM,
                TRIGGER, STATUS, START_USER, TRIGGER_USER, QUEUE_TIME,
                BUILD_PARAMETERS
            ).values(
                buildId, projectId, pipelineId, version, buildNum,
                trigger, status.ordinal, startUser, triggerUser, LocalDateTime.now(),
                buildParameters
            ).execute()
        }
    }

    fun getBuildInfo(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): BuildInfo? {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .fetchOne()?.let { convert(it) }
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        status: BuildStatus,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null
    ): Int {
        return with(T_PIPELINE_BUILD_HISTORY) {
            val update = dslContext.update(this)
                .set(STATUS, status.ordinal)
            startTime?.let { update.set(START_TIME, it) }
            endTime?.let { update.set(END_TIME, it) }
            update.where(PROJECT_ID.eq(projectId))
                .and(BUILD_ID.eq(buildId))
                .execute()
        }
    }

    fun listBuildHistory(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): List<BuildHistory> {
        return with(T_PIPELINE_BUILD_HISTORY) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .orderBy(BUILD_NUM.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
                .map { convertToHistory(it) }
        }
    }
}
```

### 3.3 PipelineResourceDao

**文件**: `PipelineResourceDao.kt` (12KB)

```kotlin
@Repository
class PipelineResourceDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        model: String,
        creator: String
    ) {
        with(T_PIPELINE_RESOURCE) {
            dslContext.insertInto(this,
                PROJECT_ID, PIPELINE_ID, VERSION, MODEL, CREATOR, CREATE_TIME
            ).values(
                projectId, pipelineId, version, model, creator, LocalDateTime.now()
            ).execute()
        }
    }

    fun getModel(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int? = null
    ): String? {
        return with(T_PIPELINE_RESOURCE) {
            val query = dslContext.select(MODEL)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
            version?.let { query.and(VERSION.eq(it)) }
                ?: query.orderBy(VERSION.desc()).limit(1)
            query.fetchOne()?.value1()
        }
    }
}
```

## 四、数据库表关系图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         T_PIPELINE_INFO                                  │
│                      (流水线基本信息)                                     │
│  PIPELINE_ID ──────────────────────────────────────────────────────────┐ │
└─────────────────────────────────────────────────────────────────────────┘ │
         │                                                                  │
         │ 1:N                                                              │
         ▼                                                                  │
┌─────────────────────────────────────────────────────────────────────────┐ │
│                         T_PIPELINE_RESOURCE                              │ │
│                      (流水线模型/版本)                                    │ │
│  PIPELINE_ID + VERSION (PK)                                             │ │
│  MODEL (JSON) ← 存储完整的流水线编排模型                                  │ │
└─────────────────────────────────────────────────────────────────────────┘ │
         │                                                                  │
         │ 1:1                                                              │
         ▼                                                                  │
┌─────────────────────────────────────────────────────────────────────────┐ │
│                         T_PIPELINE_BUILD_SUMMARY                         │ │
│                      (构建摘要/统计)                                      │ │
│  PIPELINE_ID (PK) ← 每个流水线一条记录                                   │ │
│  BUILD_NUM ← 自增构建号                                                  │ │
│  LATEST_BUILD_ID ← 最新构建                                              │ │
└─────────────────────────────────────────────────────────────────────────┘ │
         │                                                                  │
         │ 1:N                                                              │
         ▼                                                                  │
┌─────────────────────────────────────────────────────────────────────────┐ │
│                         T_PIPELINE_BUILD_HISTORY                         │◄┘
│                      (构建历史)                                          │
│  BUILD_ID (PK)                                                          │
│  PIPELINE_ID (FK) ────────────────────────────────────────────────────┘
│  BUILD_NUM, STATUS, START_TIME, END_TIME                                │
└─────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         T_PIPELINE_BUILD_STAGE                           │
│                      (构建阶段)                                          │
│  BUILD_ID + STAGE_ID (PK)                                               │
│  SEQ, STATUS, CONDITIONS                                                │
└─────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         T_PIPELINE_BUILD_CONTAINER                       │
│                      (构建容器/Job)                                      │
│  BUILD_ID + CONTAINER_ID (PK)                                           │
│  STAGE_ID (FK), SEQ, STATUS, CONTAINER_TYPE                             │
└─────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         T_PIPELINE_BUILD_TASK                            │
│                      (构建任务/插件)                                      │
│  BUILD_ID + TASK_ID (PK)                                                │
│  CONTAINER_ID (FK), TASK_SEQ, STATUS, TASK_PARAMS                       │
└─────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         T_PIPELINE_BUILD_VAR                             │
│                      (构建变量)                                          │
│  BUILD_ID + KEY (PK)                                                    │
│  VALUE, VAR_TYPE, READ_ONLY                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

## 五、RECORD 系列表（构建执行记录）

> **重要**: BK-CI 有两套构建记录表，BUILD 系列用于引擎执行调度，RECORD 系列用于前端展示和历史查询。

### 5.1 两套表的设计目的

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        BUILD 系列表（引擎执行用）                             │
│  T_PIPELINE_BUILD_STAGE / CONTAINER / TASK                                  │
│  • 用于引擎调度和执行控制                                                     │
│  • 记录当前构建的实时状态                                                     │
│  • 每次构建只有一份记录（重试时覆盖更新）                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 区别
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RECORD 系列表（历史记录用）                            │
│  T_PIPELINE_BUILD_RECORD_MODEL / STAGE / CONTAINER / TASK                   │
│  • 用于前端展示和历史查询                                                     │
│  • 支持多次执行记录（EXECUTE_COUNT 区分）                                     │
│  • 保留每次执行的完整快照                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 T_PIPELINE_BUILD_RECORD_MODEL - 构建模型记录

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_BUILD_RECORD_MODEL` (
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL DEFAULT '' COMMENT '流水线ID',
    `RESOURCE_VERSION` int(11) NOT NULL COMMENT '编排版本',
    `BUILD_NUM` int(20) NOT NULL COMMENT '构建次数',
    `EXECUTE_COUNT` int(11) NOT NULL COMMENT '执行次数',
    `START_USER` varchar(32) NOT NULL DEFAULT '' COMMENT '启动者',
    `MODEL_VAR` mediumtext NOT NULL COMMENT '当次执行的变量记录',
    `START_TYPE` varchar(32) NOT NULL DEFAULT '' COMMENT '触发方式',
    `QUEUE_TIME` datetime(3) NOT NULL COMMENT '触发时间',
    `START_TIME` datetime(3) NULL COMMENT '启动时间',
    `END_TIME` datetime(3) NULL COMMENT '结束时间',
    `STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
    `ERROR_INFO` text COMMENT '错误信息',
    `CANCEL_USER` varchar(32) DEFAULT NULL COMMENT '取消者',
    `TIMESTAMPS` text COMMENT '运行中产生的时间戳集合',
    PRIMARY KEY (`BUILD_ID`, `EXECUTE_COUNT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建详情表';
```

**关键字段说明**:

| 字段 | 说明 |
|------|------|
| `EXECUTE_COUNT` | **执行次数**，支持同一构建多次重试的记录 |
| `RESOURCE_VERSION` | 使用的流水线编排版本 |
| `MODEL_VAR` | 模型级别的变量（如 timeCost） |
| `TIMESTAMPS` | 时间戳集合（排队、启动、结束等） |

### 5.3 T_PIPELINE_BUILD_RECORD_STAGE - 阶段记录

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_BUILD_RECORD_STAGE` (
    `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
    `PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '项目ID',
    `PIPELINE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '流水线ID',
    `RESOURCE_VERSION` int(11) DEFAULT NULL COMMENT '编排版本号',
    `STAGE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '步骤ID',
    `SEQ` int(11) NOT NULL COMMENT '步骤序列',
    `STAGE_VAR` text NOT NULL COMMENT '当次执行的变量记录',
    `STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
    `EXECUTE_COUNT` int(11) NOT NULL DEFAULT '1' COMMENT '执行次数',
    `START_TIME` datetime(3) NULL COMMENT '开始时间',
    `END_TIME` datetime(3) NULL COMMENT '结束时间',
    `TIMESTAMPS` text COMMENT '运行中产生的时间戳集合',
    PRIMARY KEY (`BUILD_ID`, `STAGE_ID`, `EXECUTE_COUNT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建阶段表';
```

### 5.4 T_PIPELINE_BUILD_RECORD_CONTAINER - 容器记录

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_BUILD_RECORD_CONTAINER` (
    `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
    `RESOURCE_VERSION` int(11) NOT NULL COMMENT '编排版本',
    `STAGE_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '步骤ID',
    `CONTAINER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '构建容器ID',
    `EXECUTE_COUNT` int(11) NOT NULL DEFAULT '1' COMMENT '执行次数',
    `STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
    `CONTAINER_VAR` mediumtext NOT NULL COMMENT '当次执行的变量记录',
    `CONTAINER_TYPE` varchar(45) DEFAULT NULL COMMENT '容器类型',
    `CONTAIN_POST_TASK` bit(1) DEFAULT NULL COMMENT '包含POST插件标识',
    `MATRIX_GROUP_FLAG` bit(1) DEFAULT NULL COMMENT '矩阵标识',
    `MATRIX_GROUP_ID` varchar(64) DEFAULT NULL COMMENT '所属的矩阵组ID',
    `START_TIME` datetime(3) NULL COMMENT '开始时间',
    `END_TIME` datetime(3) NULL COMMENT '结束时间',
    `TIMESTAMPS` text COMMENT '运行中产生的时间戳集合',
    PRIMARY KEY (`BUILD_ID`, `CONTAINER_ID`, `EXECUTE_COUNT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建容器环境表';
```

**关键字段说明**:

| 字段 | 说明 |
|------|------|
| `CONTAINER_VAR` | 容器级别的变量（如 name、dispatchType） |
| `MATRIX_GROUP_FLAG` | 是否是矩阵组 |
| `CONTAIN_POST_TASK` | 是否包含 POST 插件 |

### 5.5 T_PIPELINE_BUILD_RECORD_TASK - 任务记录

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_BUILD_RECORD_TASK` (
    `BUILD_ID` varchar(34) NOT NULL COMMENT '构建ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `RESOURCE_VERSION` int(11) NOT NULL COMMENT '编排版本号',
    `STAGE_ID` varchar(34) NOT NULL DEFAULT '' COMMENT '步骤ID',
    `CONTAINER_ID` varchar(34) NOT NULL COMMENT '构建容器ID',
    `TASK_ID` varchar(34) NOT NULL COMMENT '任务ID',
    `TASK_SEQ` int(11) NOT NULL DEFAULT '1' COMMENT '任务序列',
    `EXECUTE_COUNT` int(11) NOT NULL DEFAULT '1' COMMENT '执行次数',
    `STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
    `TASK_VAR` mediumtext NOT NULL COMMENT '当次执行的变量记录',
    `POST_INFO` text DEFAULT NULL COMMENT '市场插件的POST关联信息',
    `CLASS_TYPE` varchar(64) NOT NULL DEFAULT '' COMMENT '类类型',
    `ATOM_CODE` varchar(128) NOT NULL DEFAULT '' COMMENT '插件的唯一标识',
    `ORIGIN_CLASS_TYPE` varchar(64) DEFAULT NULL COMMENT '原始类类型',
    `START_TIME` datetime(3) NULL COMMENT '开始时间',
    `END_TIME` datetime(3) NULL COMMENT '结束时间',
    `TIMESTAMPS` text COMMENT '运行中产生的时间戳集合',
    `ASYNC_STATUS` varchar(32) NULL COMMENT '插件异步执行状态',
    PRIMARY KEY (`BUILD_ID`, `TASK_ID`, `EXECUTE_COUNT`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线构建任务表';
```

**关键字段说明**:

| 字段 | 说明 |
|------|------|
| `TASK_VAR` | 任务级别的变量（插件参数、输出等） |
| `POST_INFO` | POST 插件关联信息 |
| `ASYNC_STATUS` | 异步执行状态 |

### 5.6 BUILD vs RECORD 使用场景对比

| 场景 | BUILD 表 | RECORD 表 |
|------|---------|-----------|
| 引擎调度执行 | ✅ 使用 | ❌ 不用 |
| Worker 拉取任务 | ✅ 使用 | ❌ 不用 |
| 前端构建详情页 | ❌ 不用 | ✅ 使用 |
| 查看历史执行记录 | ❌ 不用 | ✅ 使用 |
| 重试时保留历史 | ❌ 会覆盖 | ✅ 新增记录 |
| 耗时统计 | ❌ 不准确 | ✅ 精确 |

### 5.7 RECORD 系列表的 DAO 和 Service

**DAO 层**:

| DAO 类 | 表 | 路径 |
|--------|-----|------|
| `BuildRecordModelDao` | `T_PIPELINE_BUILD_RECORD_MODEL` | `biz-base/.../dao/record/` |
| `BuildRecordStageDao` | `T_PIPELINE_BUILD_RECORD_STAGE` | `biz-base/.../dao/record/` |
| `BuildRecordContainerDao` | `T_PIPELINE_BUILD_RECORD_CONTAINER` | `biz-base/.../dao/record/` |
| `BuildRecordTaskDao` | `T_PIPELINE_BUILD_RECORD_TASK` | `biz-base/.../dao/record/` |

**Service 层**:

| Service 类 | 职责 |
|------------|------|
| `PipelineBuildRecordService` | 构建记录管理，查询 ModelRecord |
| `StageBuildRecordService` | Stage 记录更新 |
| `ContainerBuildRecordService` | Container 记录更新 |
| `TaskBuildRecordService` | Task 记录更新 |
| `BaseBuildRecordService` | 基础服务，公共方法 |

### 5.8 完整的表关系图（含 RECORD 系列）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              构建启动时创建                                   │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌──────────────────────────────────────┐
                    │     T_PIPELINE_BUILD_HISTORY         │
                    │     (构建历史 - 顶层入口)              │
                    └───────────────┬──────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────────────┐
│ T_PIPELINE_BUILD_ │   │ T_PIPELINE_BUILD_ │   │ T_PIPELINE_BUILD_RECORD_  │
│ DETAIL            │   │ VAR               │   │ MODEL                     │
│ (完整Model JSON)   │   │ (构建变量)         │   │ (执行记录-支持多次)         │
└───────────────────┘   └───────────────────┘   └─────────────┬─────────────┘
                                                              │
                    ┌─────────────────────────────────────────┤
                    │                                         │
        ┌───────────┴───────────┐                 ┌───────────┴───────────┐
        │   引擎执行用 (BUILD)    │                 │   历史记录用 (RECORD)   │
        │   单次执行状态          │                 │   多次执行快照          │
        └───────────┬───────────┘                 └───────────┬───────────┘
                    │                                         │
    ┌───────────────┼───────────────┐         ┌───────────────┼───────────────┐
    ▼               ▼               ▼         ▼               ▼               ▼
┌────────┐    ┌────────┐    ┌────────┐   ┌────────┐    ┌────────┐    ┌────────┐
│ STAGE  │    │CONTAINER│   │ TASK   │   │ STAGE  │    │CONTAINER│   │ TASK   │
│(阶段)   │    │(Job)    │   │(插件)   │   │(阶段)   │    │(Job)    │   │(插件)   │
└────────┘    └────────┘    └────────┘   └────────┘    └────────┘    └────────┘
     │             │             │            │             │             │
     └─────────────┴─────────────┘            └─────────────┴─────────────┘
              引擎实时更新                           前端查询展示
```

### 5.9 EXECUTE_COUNT 的作用

```
场景：用户对构建 #100 进行了 3 次重试

BUILD 系列表：
┌─────────────────────────────────────────┐
│ BUILD_ID = xxx                          │
│ 只保留最新一次执行的状态                   │
│ （第3次重试的状态）                        │
└─────────────────────────────────────────┘

RECORD 系列表：
┌─────────────────────────────────────────┐
│ BUILD_ID = xxx, EXECUTE_COUNT = 1       │  ← 第1次执行
├─────────────────────────────────────────┤
│ BUILD_ID = xxx, EXECUTE_COUNT = 2       │  ← 第2次执行（重试）
├─────────────────────────────────────────┤
│ BUILD_ID = xxx, EXECUTE_COUNT = 3       │  ← 第3次执行（重试）
└─────────────────────────────────────────┘
```

## 六、开发规范

### 6.1 DAO 编写规范

```kotlin
@Repository
class XxxDao {
    
    // 1. 使用 with(TABLE) 简化代码
    fun create(dslContext: DSLContext, ...) {
        with(T_XXX) {
            dslContext.insertInto(this, ...)
                .values(...)
                .execute()
        }
    }
    
    // 2. 查询方法返回可空类型
    fun get(dslContext: DSLContext, id: String): XxxRecord? {
        return with(T_XXX) {
            dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }
    
    // 3. 列表查询使用 fetch()
    fun list(dslContext: DSLContext, ...): List<Xxx> {
        return with(T_XXX) {
            dslContext.selectFrom(this)
                .where(...)
                .fetch()
                .map { convert(it) }
        }
    }
    
    // 4. 更新/删除返回影响行数
    fun update(dslContext: DSLContext, ...): Int {
        return with(T_XXX) {
            dslContext.update(this)
                .set(...)
                .where(...)
                .execute()
        }
    }
}
```

### 6.2 新增表检查清单

- [ ] 表名以 `T_PIPELINE_` 开头
- [ ] 主键使用 `BUILD_ID` + 业务 ID 组合
- [ ] 添加 `PROJECT_ID`、`PIPELINE_ID` 索引
- [ ] 添加必要的时间字段（CREATE_TIME、UPDATE_TIME）
- [ ] SQL 脚本放在 `support-files/sql/` 目录

### 6.3 RECORD 表使用注意事项

- [ ] 查询前端展示数据时优先使用 RECORD 表
- [ ] 引擎调度逻辑使用 BUILD 表
- [ ] 重试场景需要同时更新两套表
- [ ] EXECUTE_COUNT 字段用于区分多次执行

---

**版本**: 2.0.0 | **更新日期**: 2025-12-12 | **更新内容**: 新增 RECORD 系列表（BUILD_RECORD_MODEL/STAGE/CONTAINER/TASK）详解
