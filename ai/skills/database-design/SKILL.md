---
name: database-design
description: BK-CI 数据库设计规范与表结构指南，涵盖命名规范、字段类型选择、索引设计、分表策略、数据归档。当用户设计数据库表、优化索引、规划分表策略或进行数据库架构设计时使用。
---

# BK-CI 数据库设计规范与表结构指南

## 一、数据库架构概述

### 1.1 数据库分布

BK-CI 采用微服务架构，每个服务拥有独立的数据库：

| 数据库名 | 所属服务 | 说明 |
|----------|----------|------|
| `devops_ci_process` | Process | 流水线核心数据，表最多（89KB DDL） |
| `devops_ci_store` | Store | 研发商店数据（87KB DDL） |
| `devops_ci_auth` | Auth | 权限认证数据（33KB DDL） |
| `devops_ci_project` | Project | 项目管理数据（25KB DDL） |
| `devops_ci_quality` | Quality | 质量红线数据（25KB DDL） |
| `devops_ci_dispatch` | Dispatch | 构建调度数据（24KB DDL） |
| `devops_ci_repository` | Repository | 代码库数据（19KB DDL） |
| `devops_ci_metrics` | Metrics | 度量数据（20KB DDL） |
| `devops_ci_environment` | Environment | 构建机环境数据（14KB DDL） |
| `devops_ci_notify` | Notify | 通知服务数据（14KB DDL） |
| `devops_ci_ticket` | Ticket | 凭证管理数据（5KB DDL） |
| `devops_ci_artifactory` | Artifactory | 制品库数据（4KB DDL） |
| `devops_ci_openapi` | OpenAPI | 开放接口数据（3KB DDL） |
| `devops_ci_log` | Log | 日志服务数据（2KB DDL） |

### 1.2 SQL 脚本组织规范

```
support-files/sql/
├── 0001_ci_create-database_mysql.sql      # 创建所有数据库
├── 1001_ci_*_ddl_mysql.sql                # 各模块完整 DDL
├── 2001_v0.x/                             # v0.x 版本增量更新
├── 2002_v1.x/                             # v1.x 版本增量更新
├── 2003_v2.x/                             # v2.x 版本增量更新
├── 2004_v3.x/                             # v3.x 版本增量更新
├── 2025_v4.x/                             # v4.x 版本增量更新
└── 5001_init_dml/                         # 初始化数据
```

**命名规范**：
- 创建数据库：`0001_{系统}_create-database_{db类型}.sql`
- 完整 DDL：`1xxx_{系统}_{模块}_ddl_{db类型}.sql`
- 增量更新：`2xxx_{系统}_{模块}_update_{版本号}_{db类型}.sql`
- 初始化数据：`5001_{系统}_{模块}_dml_{db类型}.sql`

## 二、核心表结构详解

### 2.1 Process 模块（流水线核心）

#### 流水线信息表

```sql
-- T_PIPELINE_INFO：流水线基本信息
CREATE TABLE T_PIPELINE_INFO (
  PIPELINE_ID varchar(34) PRIMARY KEY,        -- 流水线ID（P-32位UUID）
  PROJECT_ID varchar(64) NOT NULL,            -- 项目ID
  PIPELINE_NAME varchar(255) NOT NULL,        -- 流水线名称
  VERSION int(11) DEFAULT 1,                  -- 版本号
  CHANNEL varchar(32),                        -- 渠道（BS/CODECC/AM等）
  CREATOR varchar(64) NOT NULL,               -- 创建者
  CREATE_TIME timestamp NOT NULL,             -- 创建时间
  LAST_MODIFY_USER varchar(64) NOT NULL,      -- 最近修改者
  UPDATE_TIME timestamp,                      -- 更新时间
  `DELETE` bit(1) DEFAULT b'0',               -- 软删除标记
  LOCKED bit(1) DEFAULT b'0',                 -- 锁定标记（PAC）
  UNIQUE KEY (PROJECT_ID, PIPELINE_NAME)
);

-- T_PIPELINE_RESOURCE：流水线编排资源
CREATE TABLE T_PIPELINE_RESOURCE (
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(34) NOT NULL,
  VERSION int(11) NOT NULL DEFAULT 1,         -- 版本号
  MODEL mediumtext,                           -- 流水线模型（JSON）
  YAML mediumtext,                            -- YAML 编排
  CREATOR varchar(64),
  CREATE_TIME timestamp NOT NULL,
  PRIMARY KEY (PIPELINE_ID, VERSION)
);

-- T_PIPELINE_SETTING：流水线配置
CREATE TABLE T_PIPELINE_SETTING (
  PIPELINE_ID varchar(34) PRIMARY KEY,
  PROJECT_ID varchar(64),
  NAME varchar(255),
  RUN_LOCK_TYPE int(11) DEFAULT 1,            -- 运行锁类型
  WAIT_QUEUE_TIME_SECOND int(11) DEFAULT 7200,-- 最大排队时长
  MAX_QUEUE_SIZE int(11) DEFAULT 10,          -- 最大排队数量
  CONCURRENCY_GROUP varchar(255),             -- 并发组
  BUILD_NUM_RULE varchar(512),                -- 构建号规则
  SUCCESS_SUBSCRIPTION text,                  -- 成功订阅
  FAILURE_SUBSCRIPTION text                   -- 失败订阅
);
```

#### 构建历史表

```sql
-- T_PIPELINE_BUILD_HISTORY：构建历史（核心表）
CREATE TABLE T_PIPELINE_BUILD_HISTORY (
  BUILD_ID varchar(34) PRIMARY KEY,           -- 构建ID（b-32位UUID）
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(34) NOT NULL,
  BUILD_NUM int(20) DEFAULT 0,                -- 构建号
  VERSION int(11),                            -- 编排版本号
  STATUS int(11),                             -- 构建状态
  START_USER varchar(64),                     -- 启动者
  TRIGGER varchar(32) NOT NULL,               -- 触发方式
  TRIGGER_USER varchar(64),                   -- 触发者
  START_TIME timestamp,                       -- 开始时间
  END_TIME timestamp,                         -- 结束时间
  EXECUTE_TIME bigint(20),                    -- 执行时长
  MATERIAL mediumtext,                        -- 代码材料
  BUILD_PARAMETERS mediumtext,                -- 构建参数
  ERROR_TYPE int(11),                         -- 错误类型
  ERROR_CODE int(11),                         -- 错误码
  ERROR_MSG text,                             -- 错误信息
  
  -- 索引设计
  KEY STATUS_KEY (PROJECT_ID, PIPELINE_ID, STATUS),
  KEY INX_PROJECT_PIPELINE_NUM (PROJECT_ID, PIPELINE_ID, BUILD_NUM),
  KEY INX_PROJECT_PIPELINE_START_TIME (PROJECT_ID, PIPELINE_ID, START_TIME),
  KEY inx_status (STATUS),
  KEY inx_start_time (START_TIME)
);

-- T_PIPELINE_BUILD_SUMMARY：构建摘要（聚合表）
CREATE TABLE T_PIPELINE_BUILD_SUMMARY (
  PIPELINE_ID varchar(34) PRIMARY KEY,
  PROJECT_ID varchar(64) NOT NULL,
  BUILD_NUM int(11) DEFAULT 0,                -- 当前构建号
  FINISH_COUNT int(11) DEFAULT 0,             -- 完成次数
  RUNNING_COUNT int(11) DEFAULT 0,            -- 运行中次数
  QUEUE_COUNT int(11) DEFAULT 0,              -- 排队次数
  LATEST_BUILD_ID varchar(34),                -- 最近构建ID
  LATEST_STATUS int(11),                      -- 最近状态
  LATEST_START_TIME timestamp,                -- 最近开始时间
  LATEST_END_TIME timestamp                   -- 最近结束时间
);
```

#### 构建任务表

```sql
-- T_PIPELINE_BUILD_TASK：构建任务详情
CREATE TABLE T_PIPELINE_BUILD_TASK (
  BUILD_ID varchar(34) NOT NULL,
  TASK_ID varchar(34) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(34) NOT NULL,
  STAGE_ID varchar(34) NOT NULL,
  CONTAINER_ID varchar(34) NOT NULL,
  TASK_NAME varchar(128),
  TASK_TYPE varchar(64) NOT NULL,             -- 任务类型
  TASK_ATOM varchar(128),                     -- 插件标识
  ATOM_CODE varchar(128),                     -- 插件代码
  STATUS int(11),                             -- 任务状态
  START_TIME timestamp,
  END_TIME timestamp,
  EXECUTE_COUNT int(11) DEFAULT 0,            -- 执行次数
  ERROR_TYPE int(11),
  ERROR_CODE int(11),
  ERROR_MSG text,
  TASK_PARAMS mediumtext,                     -- 任务参数（JSON）
  
  PRIMARY KEY (BUILD_ID, TASK_ID),
  KEY PROJECT_PIPELINE (PROJECT_ID, PIPELINE_ID)
);

-- T_PIPELINE_BUILD_STAGE：构建阶段
CREATE TABLE T_PIPELINE_BUILD_STAGE (
  BUILD_ID varchar(64) NOT NULL,
  STAGE_ID varchar(64) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(64) NOT NULL,
  SEQ int(11) NOT NULL,                       -- 阶段序号
  STATUS int(11),
  START_TIME timestamp,
  END_TIME timestamp,
  CHECK_IN mediumtext,                        -- 准入检查
  CHECK_OUT mediumtext,                       -- 准出检查
  PRIMARY KEY (BUILD_ID, STAGE_ID)
);

-- T_PIPELINE_BUILD_CONTAINER：构建容器
CREATE TABLE T_PIPELINE_BUILD_CONTAINER (
  BUILD_ID varchar(64) NOT NULL,
  STAGE_ID varchar(64) NOT NULL,
  CONTAINER_ID varchar(64) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(64) NOT NULL,
  CONTAINER_TYPE varchar(45),                 -- 容器类型
  SEQ int(11) NOT NULL,
  STATUS int(11),
  MATRIX_GROUP_FLAG BIT(1),                   -- 是否矩阵构建
  MATRIX_GROUP_ID varchar(64),                -- 矩阵组ID
  JOB_ID varchar(128),
  PRIMARY KEY (BUILD_ID, STAGE_ID, CONTAINER_ID)
);

-- T_PIPELINE_BUILD_VAR：构建变量
CREATE TABLE T_PIPELINE_BUILD_VAR (
  BUILD_ID varchar(34) NOT NULL,
  `KEY` varchar(255) NOT NULL,                -- 变量名
  `VALUE` varchar(4000),                      -- 变量值
  PROJECT_ID varchar(64),
  PIPELINE_ID varchar(64),
  VAR_TYPE VARCHAR(64),                       -- 变量类型
  READ_ONLY bit(1),                           -- 是否只读
  PRIMARY KEY (BUILD_ID, `KEY`)
);
```

#### 构建执行记录表（RECORD 系列）

> **重要**: BK-CI 有两套构建记录表，BUILD 系列用于引擎执行调度，RECORD 系列用于前端展示和历史查询。
> RECORD 系列通过 `EXECUTE_COUNT` 字段支持同一构建的多次重试记录。

```sql
-- T_PIPELINE_BUILD_RECORD_MODEL：构建模型记录（支持多次执行）
CREATE TABLE T_PIPELINE_BUILD_RECORD_MODEL (
  BUILD_ID varchar(34) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(34) NOT NULL,
  RESOURCE_VERSION int(11) NOT NULL,          -- 编排版本
  BUILD_NUM int(20) NOT NULL,                 -- 构建次数
  EXECUTE_COUNT int(11) NOT NULL,             -- 执行次数（重试时递增）
  START_USER varchar(32) NOT NULL,
  MODEL_VAR mediumtext NOT NULL,              -- 模型级别变量（JSON）
  START_TYPE varchar(32) NOT NULL,            -- 触发方式
  QUEUE_TIME datetime(3) NOT NULL,
  START_TIME datetime(3) NULL,
  END_TIME datetime(3) NULL,
  STATUS varchar(32),
  ERROR_INFO text,                            -- 错误信息
  CANCEL_USER varchar(32),
  TIMESTAMPS text,                            -- 时间戳集合
  PRIMARY KEY (BUILD_ID, EXECUTE_COUNT)
);

-- T_PIPELINE_BUILD_RECORD_STAGE：阶段记录
CREATE TABLE T_PIPELINE_BUILD_RECORD_STAGE (
  BUILD_ID varchar(64) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(64) NOT NULL,
  RESOURCE_VERSION int(11),
  STAGE_ID varchar(64) NOT NULL,
  SEQ int(11) NOT NULL,
  STAGE_VAR text NOT NULL,                    -- 阶段级别变量
  STATUS varchar(32),
  EXECUTE_COUNT int(11) NOT NULL DEFAULT 1,
  START_TIME datetime(3) NULL,
  END_TIME datetime(3) NULL,
  TIMESTAMPS text,
  PRIMARY KEY (BUILD_ID, STAGE_ID, EXECUTE_COUNT)
);

-- T_PIPELINE_BUILD_RECORD_CONTAINER：容器记录
CREATE TABLE T_PIPELINE_BUILD_RECORD_CONTAINER (
  BUILD_ID varchar(64) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(64) NOT NULL,
  RESOURCE_VERSION int(11) NOT NULL,
  STAGE_ID varchar(64) NOT NULL,
  CONTAINER_ID varchar(64) NOT NULL,
  EXECUTE_COUNT int(11) NOT NULL DEFAULT 1,
  STATUS varchar(32),
  CONTAINER_VAR mediumtext NOT NULL,          -- 容器级别变量
  CONTAINER_TYPE varchar(45),
  CONTAIN_POST_TASK bit(1),                   -- 包含POST插件标识
  MATRIX_GROUP_FLAG bit(1),                   -- 矩阵标识
  MATRIX_GROUP_ID varchar(64),
  START_TIME datetime(3) NULL,
  END_TIME datetime(3) NULL,
  TIMESTAMPS text,
  PRIMARY KEY (BUILD_ID, CONTAINER_ID, EXECUTE_COUNT)
);

-- T_PIPELINE_BUILD_RECORD_TASK：任务记录
CREATE TABLE T_PIPELINE_BUILD_RECORD_TASK (
  BUILD_ID varchar(34) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  PIPELINE_ID varchar(34) NOT NULL,
  RESOURCE_VERSION int(11) NOT NULL,
  STAGE_ID varchar(34) NOT NULL,
  CONTAINER_ID varchar(34) NOT NULL,
  TASK_ID varchar(34) NOT NULL,
  TASK_SEQ int(11) NOT NULL DEFAULT 1,
  EXECUTE_COUNT int(11) NOT NULL DEFAULT 1,
  STATUS varchar(32),
  TASK_VAR mediumtext NOT NULL,               -- 任务级别变量
  POST_INFO text,                             -- POST插件关联信息
  CLASS_TYPE varchar(64) NOT NULL,
  ATOM_CODE varchar(128) NOT NULL,
  ORIGIN_CLASS_TYPE varchar(64),
  START_TIME datetime(3) NULL,
  END_TIME datetime(3) NULL,
  TIMESTAMPS text,
  ASYNC_STATUS varchar(32),                   -- 异步执行状态
  PRIMARY KEY (BUILD_ID, TASK_ID, EXECUTE_COUNT)
);
```

**BUILD vs RECORD 表使用场景**:

| 场景 | BUILD 表 | RECORD 表 |
|------|---------|-----------|
| 引擎调度执行 | ✅ | ❌ |
| Worker 拉取任务 | ✅ | ❌ |
| 前端构建详情页 | ❌ | ✅ |
| 查看历史执行记录 | ❌ | ✅ |
| 重试时保留历史 | ❌ 覆盖 | ✅ 新增 |

#### 模板相关表

```sql
-- T_TEMPLATE：流水线模板
CREATE TABLE T_TEMPLATE (
  VERSION bigint(20) AUTO_INCREMENT,          -- 模板版本
  ID varchar(32) NOT NULL,                    -- 模板ID
  TEMPLATE_NAME varchar(64) NOT NULL,
  PROJECT_ID varchar(34) NOT NULL,
  VERSION_NAME varchar(64) NOT NULL,
  TEMPLATE mediumtext,                        -- 模板内容（JSON）
  TYPE varchar(32) DEFAULT 'CUSTOMIZE',       -- 类型
  STORE_FLAG bit(1) DEFAULT b'0',             -- 是否关联商店
  PRIMARY KEY (VERSION),
  KEY ID (ID)
);

-- T_TEMPLATE_PIPELINE：模板实例关联
CREATE TABLE T_TEMPLATE_PIPELINE (
  PIPELINE_ID varchar(34) PRIMARY KEY,
  PROJECT_ID varchar(64) NOT NULL,
  TEMPLATE_ID varchar(32) NOT NULL,
  VERSION bigint(20) NOT NULL,
  INSTANCE_TYPE VARCHAR(32) DEFAULT 'CONSTRAINT', -- FREEDOM/CONSTRAINT
  PARAM mediumtext,                           -- 实例参数
  STATUS varchar(32) DEFAULT 'UPDATED'
);
```

### 2.2 Project 模块（项目管理）

```sql
-- T_PROJECT：项目信息（核心表）
CREATE TABLE T_PROJECT (
  ID bigint(20) AUTO_INCREMENT,
  PROJECT_ID varchar(32) NOT NULL,            -- 项目ID（英文名）
  project_name varchar(64) NOT NULL,          -- 项目名称
  english_name varchar(64) NOT NULL,          -- 英文名称
  creator varchar(32),                        -- 创建者
  description text,                           -- 描述
  is_offlined bit(1) DEFAULT b'0',            -- 是否停用
  bg_id bigint(20),                           -- 事业群ID
  bg_name varchar(255),                       -- 事业群名称
  dept_id bigint(20),                         -- 部门ID
  dept_name varchar(255),                     -- 部门名称
  center_id bigint(20),                       -- 中心ID
  center_name varchar(255),                   -- 中心名称
  approval_status int(10) DEFAULT 1,          -- 审核状态
  CHANNEL varchar(32) DEFAULT 'BS',           -- 渠道
  pipeline_limit int(10) DEFAULT 500,         -- 流水线数量上限
  router_tag varchar(32),                     -- 路由标签
  properties text,                            -- 其他配置
  
  PRIMARY KEY (ID),
  UNIQUE KEY project_name (project_name),
  UNIQUE KEY project_id (PROJECT_ID),
  UNIQUE KEY english_name (english_name)
);

-- T_SERVICE：服务信息
CREATE TABLE T_SERVICE (
  id bigint(20) AUTO_INCREMENT PRIMARY KEY,
  name varchar(64),                           -- 服务名称
  english_name varchar(64),                   -- 英文名称
  service_type_id bigint(20),                 -- 服务类型
  link varchar(255),                          -- 跳转链接
  status varchar(64),                         -- 状态
  UNIQUE KEY service_name (name)
);

-- T_SHARDING_ROUTING_RULE：分片路由规则
CREATE TABLE T_SHARDING_ROUTING_RULE (
  ID varchar(32) PRIMARY KEY,
  ROUTING_NAME varchar(128) NOT NULL,         -- 路由名称
  ROUTING_RULE varchar(256) NOT NULL,         -- 路由规则
  CLUSTER_NAME varchar(64) DEFAULT 'prod',    -- 集群名称
  MODULE_CODE varchar(64) DEFAULT 'PROCESS',  -- 模块标识
  TYPE varchar(32) DEFAULT 'DB',              -- 路由类型
  DATA_SOURCE_NAME varchar(128) DEFAULT 'ds_0'-- 数据源名称
);
```

### 2.3 Auth 模块（权限认证）

```sql
-- T_AUTH_RESOURCE：资源表
CREATE TABLE T_AUTH_RESOURCE (
  ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_CODE varchar(32) NOT NULL,
  RESOURCE_TYPE varchar(32) NOT NULL,         -- 资源类型
  RESOURCE_CODE varchar(255) NOT NULL,        -- 资源ID
  RESOURCE_NAME varchar(255) NOT NULL,        -- 资源名
  IAM_RESOURCE_CODE varchar(32) NOT NULL,     -- IAM资源ID
  ENABLE bit(1) DEFAULT b'0',                 -- 是否启用权限管理
  RELATION_ID varchar(32) NOT NULL,           -- IAM分级管理员ID
  
  UNIQUE KEY IDX_PROJECT_RESOURCE (PROJECT_CODE, RESOURCE_TYPE, RESOURCE_CODE)
);

-- T_AUTH_RESOURCE_GROUP：资源用户组
CREATE TABLE T_AUTH_RESOURCE_GROUP (
  ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_CODE varchar(32) NOT NULL,
  RESOURCE_TYPE varchar(32) NOT NULL,
  RESOURCE_CODE varchar(255) NOT NULL,
  GROUP_CODE varchar(32) NOT NULL,            -- 用户组标识
  GROUP_NAME varchar(255) NOT NULL,           -- 用户组名称
  DEFAULT_GROUP bit(1) DEFAULT b'1',          -- 是否默认组
  RELATION_ID varchar(32) NOT NULL,           -- IAM组ID
  
  UNIQUE KEY (PROJECT_CODE, RESOURCE_TYPE, RESOURCE_CODE, GROUP_NAME)
);

-- T_AUTH_RESOURCE_GROUP_MEMBER：用户组成员
CREATE TABLE T_AUTH_RESOURCE_GROUP_MEMBER (
  ID bigint AUTO_INCREMENT PRIMARY KEY,
  PROJECT_CODE varchar(64) NOT NULL,
  RESOURCE_TYPE varchar(32) NOT NULL,
  RESOURCE_CODE varchar(255) NOT NULL,
  GROUP_CODE varchar(32) NOT NULL,
  IAM_GROUP_ID int(20) NOT NULL,
  MEMBER_ID varchar(64) NOT NULL,             -- 成员ID
  MEMBER_NAME varchar(512) NOT NULL,          -- 成员名
  MEMBER_TYPE varchar(32) NOT NULL,           -- 成员类型
  EXPIRED_TIME datetime NOT NULL,             -- 过期时间
  
  UNIQUE KEY (PROJECT_CODE, IAM_GROUP_ID, MEMBER_ID)
);

-- T_AUTH_ACTION：权限操作表
CREATE TABLE T_AUTH_ACTION (
  ACTION varchar(64) PRIMARY KEY,             -- 操作ID
  RESOURCE_TYPE varchar(64) NOT NULL,         -- 关联资源类型
  ACTION_NAME varchar(64) NOT NULL,           -- 操作名称
  ACTION_TYPE varchar(32)                     -- 操作类型
);
```

### 2.4 Store 模块（研发商店）

```sql
-- T_ATOM：插件信息
CREATE TABLE T_ATOM (
  ID varchar(32) PRIMARY KEY,
  NAME varchar(64) NOT NULL,                  -- 插件名称
  ATOM_CODE varchar(64) NOT NULL,             -- 插件唯一标识
  CLASS_TYPE varchar(64) NOT NULL,            -- 插件大类
  VERSION varchar(30) NOT NULL,               -- 版本号
  ATOM_STATUS tinyint(4) NOT NULL,            -- 插件状态
  ATOM_TYPE tinyint(4) DEFAULT 1,             -- 插件类型
  OS varchar(100) NOT NULL,                   -- 支持的操作系统
  CLASSIFY_ID varchar(32) NOT NULL,           -- 分类ID
  LATEST_FLAG bit(1) NOT NULL,                -- 是否最新版本
  DEFAULT_FLAG bit(1) DEFAULT b'0',           -- 是否默认插件
  PUBLISHER varchar(50) DEFAULT 'system',     -- 发布者
  PROPS text,                                 -- 插件属性（JSON）
  
  UNIQUE KEY (ATOM_CODE, VERSION),
  KEY inx_atom_status (ATOM_STATUS),
  KEY inx_latest_flag (LATEST_FLAG)
);

-- T_ATOM_ENV_INFO：插件执行环境
CREATE TABLE T_ATOM_ENV_INFO (
  ID varchar(32) PRIMARY KEY,
  ATOM_ID varchar(32) NOT NULL,
  PKG_PATH varchar(1024) NOT NULL,            -- 安装包路径
  LANGUAGE varchar(64),                       -- 开发语言
  TARGET varchar(256) NOT NULL,               -- 执行入口
  OS_NAME varchar(128),                       -- 操作系统
  OS_ARCH varchar(128),                       -- 系统架构
  RUNTIME_VERSION varchar(128),               -- 运行时版本
  DEFAULT_FLAG bit(1) DEFAULT b'1',           -- 是否默认环境
  
  UNIQUE KEY (ATOM_ID, OS_NAME, OS_ARCH)
);

-- T_STORE_PROJECT_REL：商店组件与项目关联
CREATE TABLE T_STORE_PROJECT_REL (
  ID varchar(32) PRIMARY KEY,
  STORE_CODE varchar(64) NOT NULL,            -- 组件编码
  PROJECT_CODE varchar(64) NOT NULL,          -- 项目ID
  TYPE tinyint(4) NOT NULL,                   -- 关联类型
  STORE_TYPE tinyint(4) DEFAULT 0,            -- 组件类型
  
  UNIQUE KEY (STORE_CODE, STORE_TYPE, PROJECT_CODE, TYPE, INSTANCE_ID, CREATOR)
);

-- T_STORE_STATISTICS_TOTAL：统计信息
CREATE TABLE T_STORE_STATISTICS_TOTAL (
  ID varchar(32) PRIMARY KEY,
  STORE_CODE varchar(64) NOT NULL,
  STORE_TYPE tinyint(4) DEFAULT 0,
  DOWNLOADS int(11) DEFAULT 0,                -- 下载量
  COMMITS int(11) DEFAULT 0,                  -- 评论数
  SCORE int(11) DEFAULT 0,                    -- 评分
  SCORE_AVERAGE decimal(3,1) DEFAULT 0.0,     -- 平均分
  PIPELINE_NUM INT(11) DEFAULT 0,             -- 流水线数
  
  UNIQUE KEY (STORE_CODE, STORE_TYPE)
);
```

### 2.5 Repository 模块（代码库）

```sql
-- T_REPOSITORY：代码库信息
CREATE TABLE T_REPOSITORY (
  REPOSITORY_ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_ID varchar(32) NOT NULL,
  USER_ID varchar(64) NOT NULL,
  ALIAS_NAME varchar(255) NOT NULL,           -- 别名
  URL varchar(255) NOT NULL,                  -- 仓库URL
  TYPE varchar(20) NOT NULL,                  -- 类型（SVN/GIT/GITHUB等）
  REPOSITORY_HASH_ID varchar(64),             -- 哈希ID
  IS_DELETED bit(1) NOT NULL,                 -- 是否删除
  ENABLE_PAC bit(1) DEFAULT false,            -- 是否开启PAC
  
  KEY PROJECT_ID (PROJECT_ID),
  KEY inx_alias_name (ALIAS_NAME)
);

-- T_REPOSITORY_CODE_GIT：Git代码库详情
CREATE TABLE T_REPOSITORY_CODE_GIT (
  REPOSITORY_ID bigint(20) PRIMARY KEY,
  PROJECT_NAME varchar(255) NOT NULL,         -- 项目名称
  USER_NAME varchar(64) NOT NULL,             -- 用户名
  CREDENTIAL_ID varchar(64) NOT NULL,         -- 凭据ID
  AUTH_TYPE varchar(8),                       -- 认证方式
  GIT_PROJECT_ID bigint(20) DEFAULT 0         -- Git项目ID
);

-- T_REPOSITORY_COMMIT：提交记录
CREATE TABLE T_REPOSITORY_COMMIT (
  ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  BUILD_ID varchar(34),
  PIPELINE_ID varchar(34),
  REPO_ID bigint(20),
  TYPE smallint(6),                           -- 1-svn, 2-git, 3-gitlab
  COMMIT varchar(64),                         -- 提交ID
  COMMITTER varchar(32),                      -- 提交者
  COMMIT_TIME datetime,                       -- 提交时间
  COMMENT longtext,                           -- 提交信息
  
  KEY IDX_BUILD_ID_TIME (BUILD_ID, COMMIT_TIME)
);
```

### 2.6 Dispatch 模块（构建调度）

```sql
-- T_DISPATCH_THIRDPARTY_AGENT_BUILD：第三方构建机任务
CREATE TABLE T_DISPATCH_THIRDPARTY_AGENT_BUILD (
  ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_ID varchar(64) NOT NULL,
  AGENT_ID varchar(32) NOT NULL,              -- 构建机ID
  PIPELINE_ID varchar(34) NOT NULL,
  BUILD_ID varchar(34) NOT NULL,
  VM_SEQ_ID varchar(34) NOT NULL,             -- 构建序列号
  STATUS int(11) NOT NULL,                    -- 状态
  WORKSPACE varchar(4096),                    -- 工作空间
  NODE_ID bigint(20) DEFAULT 0,               -- 节点ID
  DOCKER_INFO json,                           -- Docker构建信息
  
  UNIQUE KEY (BUILD_ID, VM_SEQ_ID),
  KEY idx_agent_id (AGENT_ID),
  KEY idx_status (STATUS)
);

-- T_DISPATCH_RUNNING_JOBS：运行中的任务
CREATE TABLE T_DISPATCH_RUNNING_JOBS (
  ID int(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_ID varchar(128) NOT NULL,
  VM_TYPE varchar(128) NOT NULL,              -- 构建机类型
  CHANNEL_CODE varchar(128) DEFAULT 'BS',     -- 构建来源
  BUILD_ID varchar(128) NOT NULL,
  VM_SEQ_ID varchar(128) NOT NULL,
  EXECUTE_COUNT int(11) NOT NULL,             -- 执行次数
  CREATED_TIME datetime NOT NULL,
  AGENT_START_TIME datetime,                  -- 构建机启动时间
  
  KEY inx_project_id (PROJECT_ID, VM_TYPE, CHANNEL_CODE),
  KEY inx_build_id (BUILD_ID)
);

-- T_DISPATCH_QUOTA_PROJECT：项目配额
CREATE TABLE T_DISPATCH_QUOTA_PROJECT (
  PROJECT_ID varchar(128) NOT NULL,
  VM_TYPE varchar(128) NOT NULL,
  CHANNEL_CODE varchar(128) DEFAULT 'BS',
  RUNNING_JOBS_MAX int(10) NOT NULL,          -- 最大并发JOB数
  RUNNING_TIME_JOB_MAX int(10) NOT NULL,      -- 单JOB最大执行时间
  RUNNING_TIME_PROJECT_MAX int(10) NOT NULL,  -- 项目最大执行时间
  
  PRIMARY KEY (PROJECT_ID, VM_TYPE, CHANNEL_CODE)
);
```

### 2.7 Environment 模块（构建机环境）

```sql
-- T_NODE：节点信息
CREATE TABLE T_NODE (
  NODE_ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_ID varchar(64) NOT NULL,
  NODE_IP varchar(64) NOT NULL,               -- 节点IP
  NODE_NAME varchar(64) NOT NULL,             -- 节点名称
  NODE_STATUS varchar(64) NOT NULL,           -- 节点状态
  NODE_TYPE varchar(64) NOT NULL,             -- 节点类型
  OS_NAME varchar(128),                       -- 操作系统
  DISPLAY_NAME varchar(128) DEFAULT '',       -- 别名
  PIPELINE_REF_COUNT int(11) DEFAULT 0,       -- 流水线引用数
  LAST_BUILD_TIME datetime,                   -- 最近构建时间
  
  KEY PROJECT_ID (PROJECT_ID),
  KEY NODE_IP (NODE_IP)
);

-- T_ENVIRONMENT_THIRDPARTY_AGENT：第三方构建机
CREATE TABLE T_ENVIRONMENT_THIRDPARTY_AGENT (
  ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  NODE_ID bigint(20),                         -- 节点ID
  PROJECT_ID varchar(64) NOT NULL,
  HOSTNAME varchar(128) DEFAULT '',           -- 主机名
  IP varchar(64) DEFAULT '',                  -- IP地址
  OS varchar(16) NOT NULL,                    -- 操作系统
  STATUS int(11) NOT NULL,                    -- 状态
  SECRET_KEY varchar(256) NOT NULL,           -- 密钥
  VERSION varchar(128),                       -- Agent版本
  PARALLEL_TASK_COUNT int(11),                -- 并行任务数
  DOCKER_PARALLEL_TASK_COUNT int(11),         -- Docker并行任务数
  
  KEY idx_agent_node (NODE_ID),
  KEY idx_agent_project (PROJECT_ID)
);

-- T_ENV：环境信息
CREATE TABLE T_ENV (
  ENV_ID bigint(20) AUTO_INCREMENT PRIMARY KEY,
  PROJECT_ID varchar(64) NOT NULL,
  ENV_NAME varchar(128) NOT NULL,             -- 环境名称
  ENV_TYPE varchar(128) NOT NULL,             -- 环境类型（DEV/TEST/BUILD）
  ENV_VARS text NOT NULL,                     -- 环境变量
  IS_DELETED bit(1) NOT NULL,
  
  KEY PROJECT_ID (PROJECT_ID)
);

-- T_ENV_NODE：环境-节点关联
CREATE TABLE T_ENV_NODE (
  ENV_ID bigint(20) NOT NULL,
  NODE_ID bigint(20) NOT NULL,
  PROJECT_ID varchar(64) NOT NULL,
  ENABLE_NODE bit(1) DEFAULT 1,               -- 是否启用
  
  PRIMARY KEY (ENV_ID, NODE_ID)
);
```

## 三、表设计规范

### 3.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | `T_` 前缀 + 大写下划线 | `T_PIPELINE_BUILD_HISTORY` |
| 主键 | `ID` 或 `{表名}_ID` | `PIPELINE_ID`, `BUILD_ID` |
| 外键 | `{关联表}_ID` | `PROJECT_ID`, `NODE_ID` |
| 索引 | `idx_` 或 `inx_` 前缀 | `idx_project_id`, `inx_status` |
| 唯一索引 | `uni_inx_` 或 `UNI_` 前缀 | `uni_inx_code_version` |
| 时间字段 | `*_TIME` 后缀 | `CREATE_TIME`, `UPDATE_TIME` |
| 标记字段 | `*_FLAG` 后缀 | `LATEST_FLAG`, `DELETE_FLAG` |

### 3.2 ID 设计规范

```
项目ID：    英文名称（如 demo_project）
流水线ID：  p-{32位UUID} = 34位
构建ID：    b-{32位UUID} = 34位
任务ID：    t-{32位UUID} = 34位
阶段ID：    s-{32位UUID} = 34位
容器ID：    c-{32位UUID} = 34位
插件ID：    32位UUID
用户组ID：  32位UUID
```

### 3.3 状态字段设计

```sql
-- 构建状态（int 类型）
-- 参考 BuildStatus 枚举
0: QUEUE           -- 排队中
1: RUNNING         -- 运行中
2: SUCCEED         -- 成功
3: FAILED          -- 失败
4: CANCELED        -- 取消
5: TERMINATE       -- 终止
...

-- 插件状态（tinyint 类型）
0: 初始化
1: 提交中
2: 验证中
3: 验证失败
4: 测试中
5: 审核中
6: 审核驳回
7: 已发布
8: 上架中止
9: 下架中
10: 已下架
```

### 3.4 字段类型规范

| 场景 | 类型 | 说明 |
|------|------|------|
| 主键ID | `varchar(32)` 或 `bigint(20)` | UUID 用 varchar，自增用 bigint |
| 项目/流水线ID | `varchar(64)` | 预留足够长度 |
| 名称 | `varchar(64)` ~ `varchar(255)` | 根据业务需求 |
| 描述 | `varchar(1024)` 或 `text` | 短描述用 varchar |
| JSON 数据 | `mediumtext` 或 `json` | 大 JSON 用 mediumtext |
| 时间 | `datetime` 或 `timestamp` | 需要自动更新用 timestamp |
| 布尔 | `bit(1)` | 默认 `b'0'` |
| 状态 | `int(11)` 或 `tinyint(4)` | 枚举值用 tinyint |

### 3.5 索引设计规范

```sql
-- 1. 主键索引
PRIMARY KEY (`ID`)

-- 2. 唯一索引（业务唯一约束）
UNIQUE KEY `uni_inx_code_version` (`ATOM_CODE`, `VERSION`)

-- 3. 普通索引（查询优化）
KEY `idx_project_pipeline` (`PROJECT_ID`, `PIPELINE_ID`)

-- 4. 复合索引（遵循最左前缀原则）
KEY `STATUS_KEY` (`PROJECT_ID`, `PIPELINE_ID`, `STATUS`)

-- 5. 时间索引（范围查询）
KEY `inx_start_time` (`START_TIME`)
KEY `inx_create_time` (`CREATE_TIME`)
```

## 四、SQL 脚本编写规范

### 4.1 幂等性要求

```sql
-- 1. 建表必须使用 IF NOT EXISTS
CREATE TABLE IF NOT EXISTS `T_EXAMPLE` (...);

-- 2. 插入数据使用 INSERT IGNORE 防止覆盖
INSERT IGNORE INTO T_EXAMPLE (ID, NAME) VALUES (1, 'test');

-- 3. 需要强制刷新的系统数据使用 ON DUPLICATE KEY UPDATE
INSERT INTO T_EXAMPLE (ID, NAME) VALUES (1, 'test')
ON DUPLICATE KEY UPDATE NAME = 'test';

-- 4. 禁止直接删除表后重建
-- 错误：DROP TABLE IF EXISTS T_EXAMPLE; CREATE TABLE T_EXAMPLE...
-- 正确：CREATE TABLE IF NOT EXISTS T_EXAMPLE...
```

### 4.2 字段变更规范

```sql
-- 1. 新增字段必须有默认值或允许 NULL
ALTER TABLE T_EXAMPLE ADD COLUMN NEW_FIELD varchar(64) DEFAULT '';
ALTER TABLE T_EXAMPLE ADD COLUMN NEW_FIELD varchar(64) NULL;

-- 2. 禁止改名字段（会导致数据丢失）
-- 错误：ALTER TABLE T_EXAMPLE CHANGE OLD_NAME NEW_NAME varchar(64);

-- 3. 使用存储过程判断字段是否存在
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.columns 
        WHERE table_schema = DATABASE() 
        AND table_name = 'T_EXAMPLE' 
        AND column_name = 'NEW_FIELD'
    ) THEN
        ALTER TABLE T_EXAMPLE ADD COLUMN NEW_FIELD varchar(64) DEFAULT '';
    END IF;
END$$
DELIMITER ;
CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
```

### 4.3 索引变更规范

```sql
-- 添加索引（使用存储过程判断）
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_index_if_not_exists()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.statistics 
        WHERE table_schema = DATABASE() 
        AND table_name = 'T_EXAMPLE' 
        AND index_name = 'idx_name'
    ) THEN
        ALTER TABLE T_EXAMPLE ADD INDEX idx_name (NAME);
    END IF;
END$$
DELIMITER ;
CALL add_index_if_not_exists();
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
```

## 五、分库分表设计

### 5.1 分片策略

BK-CI 支持数据库分片，通过 `T_SHARDING_ROUTING_RULE` 表配置路由规则：

```sql
-- 分片路由规则
CREATE TABLE T_SHARDING_ROUTING_RULE (
  ID varchar(32) PRIMARY KEY,
  ROUTING_NAME varchar(128) NOT NULL,         -- 路由名称（项目ID）
  ROUTING_RULE varchar(256) NOT NULL,         -- 路由规则
  CLUSTER_NAME varchar(64) DEFAULT 'prod',    -- 集群名称
  MODULE_CODE varchar(64) DEFAULT 'PROCESS',  -- 模块标识
  TYPE varchar(32) DEFAULT 'DB',              -- 路由类型（DB/TABLE）
  DATA_SOURCE_NAME varchar(128) DEFAULT 'ds_0'-- 数据源名称
);
```

### 5.2 数据源配置

```sql
-- 数据源配置
CREATE TABLE T_DATA_SOURCE (
  ID varchar(32) PRIMARY KEY,
  CLUSTER_NAME varchar(64) NOT NULL,
  MODULE_CODE varchar(64) NOT NULL,
  DATA_SOURCE_NAME varchar(128) NOT NULL,
  FULL_FLAG bit(1) DEFAULT b'0',              -- 容量是否满
  DS_URL varchar(1024),                       -- 数据源URL
  TAG varchar(128),                           -- 数据源标签
  TYPE varchar(32) DEFAULT 'DB'               -- DB/ARCHIVE_DB
);
```

## 六、常用查询模式

### 6.1 流水线查询

```sql
-- 查询项目下的流水线列表
SELECT * FROM T_PIPELINE_INFO 
WHERE PROJECT_ID = ? AND `DELETE` = 0
ORDER BY CREATE_TIME DESC;

-- 查询流水线最新版本编排
SELECT * FROM T_PIPELINE_RESOURCE 
WHERE PIPELINE_ID = ? 
ORDER BY VERSION DESC LIMIT 1;

-- 查询流水线构建历史
SELECT * FROM T_PIPELINE_BUILD_HISTORY 
WHERE PROJECT_ID = ? AND PIPELINE_ID = ?
ORDER BY BUILD_NUM DESC
LIMIT ?, ?;
```

### 6.2 构建查询

```sql
-- 查询构建详情
SELECT h.*, d.MODEL 
FROM T_PIPELINE_BUILD_HISTORY h
LEFT JOIN T_PIPELINE_BUILD_DETAIL d ON h.BUILD_ID = d.BUILD_ID
WHERE h.BUILD_ID = ?;

-- 查询构建任务列表
SELECT * FROM T_PIPELINE_BUILD_TASK 
WHERE BUILD_ID = ?
ORDER BY STAGE_ID, CONTAINER_ID, TASK_SEQ;

-- 查询构建变量
SELECT * FROM T_PIPELINE_BUILD_VAR 
WHERE BUILD_ID = ?;
```

### 6.3 统计查询

```sql
-- 项目流水线数量统计
SELECT PROJECT_ID, COUNT(*) as count 
FROM T_PIPELINE_INFO 
WHERE `DELETE` = 0
GROUP BY PROJECT_ID;

-- 构建状态统计
SELECT STATUS, COUNT(*) as count 
FROM T_PIPELINE_BUILD_HISTORY 
WHERE PROJECT_ID = ? AND PIPELINE_ID = ?
GROUP BY STATUS;
```

## 七、性能优化建议

### 7.1 索引优化

1. **复合索引遵循最左前缀原则**
2. **高频查询字段建立索引**
3. **避免在索引列上使用函数**
4. **定期分析慢查询日志**

### 7.2 查询优化

1. **使用 LIMIT 限制返回行数**
2. **避免 SELECT ***
3. **使用覆盖索引**
4. **大表分页使用游标分页**

### 7.3 表设计优化

1. **大字段（TEXT/BLOB）拆分到独立表**
2. **历史数据定期归档**
3. **热点数据使用缓存**
4. **考虑读写分离**


---

## 十二、扩展主题

### 12.1 数据库脚本管理

详见 [reference/1-script-management.md](./reference/1-script-management.md)

**核心内容**:
- SQL 脚本命名规范
- 增量更新脚本编写
- 脚本执行顺序管理
- 回滚脚本设计

### 12.2 数据库分片

详见 [reference/2-sharding.md](./reference/2-sharding.md)

**核心内容**:
- 分片策略选择
- 分片键设计
- 跨分片查询处理
- 数据迁移方案
