---
name: 06-database-script-management
description: 数据库脚本管理规范，涵盖 DDL/DML 脚本编写、版本命名规则、增量更新策略、数据迁移、回滚方案。当用户编写数据库脚本、新增表结构、修改字段、进行数据迁移或管理 SQL 版本时使用。
---

# Skill 06: 数据库脚本管理

## 概述
BK-CI 使用 MySQL/MariaDB 作为主数据库，SQL 脚本按照严格的命名规范和版本管理策略组织。

## 数据库支持

| 数据库 | 版本要求 |
|--------|---------|
| MySQL | 5.7.2+ |
| MariaDB | 10.x+ |

## SQL 文件位置

```
support-files/sql/
├── 0001_ci_create-database_mysql.sql      # 创建所有数据库
├── 1001_ci_project_ddl_mysql.sql          # Project模块完整DDL
├── 1002_ci_process_ddl_mysql.sql          # Process模块完整DDL
├── 2003_v2.x/                             # v2.x版本增量更新
├── 2004_v3.x/                             # v3.x版本增量更新
├── 2025_v4.x/                             # v4.x版本增量更新
└── 5001_init_dml/                         # 初始化数据
```

## 文件命名规范

### 创建数据库脚本

```
0001_{系统}_create-database_{db类型}.sql
```

示例：`0001_ci_create-database_mysql.sql`

### 全版本 DDL 脚本

```
1xxx_{系统}_{模块}_ddl_{db类型}.sql
```

示例：
- `1001_ci_project_ddl_mysql.sql`
- `1002_ci_process_ddl_mysql.sql`
- `1003_ci_repository_ddl_mysql.sql`

### 增量版本更新脚本

```
2xxx_{系统}_{模块}_update_{版本号}_{db类型}.sql
```

示例：
- `2003_ci_process_update_v2.0.0_mysql.sql`
- `2004_ci_auth_update_v3.0.0_mysql.sql`

**⚠️ 重要：双轨更新机制**

当在增量脚本中添加/修改表结构时，**必须同步更新**对应的全版本 DDL 脚本！

### 初始化数据脚本

```
5001_{系统}_{模块}_dml_{db类型}.sql
```

示例：`5001_ci_project_dml_mysql.sql`

## 双轨更新机制（⚠️ 重要）

### 核心原则

任何表结构变更（DDL）必须在**两个位置**同步更新：

1. **增量版本更新脚本**（`2xxx_*_update_*.sql`）- 用于现有环境升级
2. **全版本 DDL 脚本**（`1xxx_*_ddl_*.sql`）- 用于全新安装

### 为什么需要双轨更新？

- **增量脚本**：负责将已部署的旧版本数据库升级到新版本
- **全量脚本**：负责全新安装时直接创建最新的完整表结构

如果只更新增量脚本，全新安装时会缺少对应的字段/索引！

### 标准变更流程

假设需要为 `T_PROJECT` 表添加索引 `IDX_PROJECT_NAME`：

#### 步骤 1：更新增量脚本

**文件**：`support-files/sql/2025_v4.x/2040_ci_project-update_v4.1_mysql.sql`

```sql
USE devops_ci_project;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- 添加索引
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT'
                    AND INDEX_NAME = 'IDX_PROJECT_NAME') THEN
        ALTER TABLE `T_PROJECT` ADD INDEX `IDX_PROJECT_NAME` (`PROJECT_NAME`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;

CALL ci_project_schema_update();
```

#### 步骤 2：同步更新全量 DDL 脚本

**文件**：`support-files/sql/1001_ci_project_ddl_mysql.sql`

找到 `T_PROJECT` 表的建表语句，在索引定义区域添加：

```sql
CREATE TABLE IF NOT EXISTS `T_PROJECT` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `PROJECT_ID` varchar(64) NOT NULL,
    `PROJECT_NAME` varchar(256) NOT NULL,
    -- ... 其他字段
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_PROJECT_ID` (`PROJECT_ID`),
    KEY `IDX_PROJECT_NAME` (`PROJECT_NAME`)  -- ✅ 新增索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 常见变更类型示例

| 变更类型 | 增量脚本操作 | 全量脚本操作 |
|----------|-------------|-------------|
| 添加列 | `ALTER TABLE ADD COLUMN` | 在 `CREATE TABLE` 中添加字段定义 |
| 添加索引 | `ALTER TABLE ADD INDEX` | 在 `CREATE TABLE` 的 `KEY` 区域添加 |
| 修改列 | `ALTER TABLE MODIFY COLUMN` | 修改 `CREATE TABLE` 中的字段定义 |
| 添加唯一约束 | `ALTER TABLE ADD UNIQUE KEY` | 在 `CREATE TABLE` 的 `UNIQUE KEY` 区域添加 |
| 添加外键 | `ALTER TABLE ADD CONSTRAINT` | 在 `CREATE TABLE` 的 `CONSTRAINT` 区域添加 |

### 检查清单

在提交数据库变更前，确认：

- [ ] 增量脚本已创建（`2xxx_*_update_*.sql`）
- [ ] 增量脚本具备幂等性（可重复执行）
- [ ] 全量 DDL 脚本已同步更新（`1xxx_*_ddl_*.sql`）
- [ ] 两处的字段定义、类型、默认值完全一致
- [ ] 索引名称、类型在两处保持一致
- [ ] 本地测试通过（全新安装 + 升级安装）

---

## 幂等性要求

所有 SQL 脚本必须支持重复执行（幂等性）：

### 创建表

```sql
-- ✅ 正确：使用 IF NOT EXISTS
CREATE TABLE IF NOT EXISTS `T_PROJECT` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `PROJECT_ID` varchar(64) NOT NULL,
    `PROJECT_NAME` varchar(256) NOT NULL,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ❌ 错误：直接创建
CREATE TABLE `T_PROJECT` (...);
```

### 添加列

```sql
-- ✅ 正确：使用存储过程检查
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'T_PROJECT' 
        AND COLUMN_NAME = 'NEW_COLUMN'
    ) THEN
        ALTER TABLE `T_PROJECT` ADD COLUMN `NEW_COLUMN` varchar(64) DEFAULT NULL;
    END IF;
END$$
DELIMITER ;
CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- ❌ 错误：直接添加
ALTER TABLE `T_PROJECT` ADD COLUMN `NEW_COLUMN` varchar(64);
```

### 插入数据

```sql
-- ✅ 正确：使用 INSERT IGNORE 或 ON DUPLICATE KEY UPDATE
INSERT IGNORE INTO `T_CONFIG` (`KEY`, `VALUE`) VALUES ('config_key', 'config_value');

INSERT INTO `T_CONFIG` (`KEY`, `VALUE`) VALUES ('config_key', 'config_value')
ON DUPLICATE KEY UPDATE `VALUE` = VALUES(`VALUE`);

-- ❌ 错误：直接插入
INSERT INTO `T_CONFIG` (`KEY`, `VALUE`) VALUES ('config_key', 'config_value');
```

### 创建索引

```sql
-- ✅ 正确：使用存储过程检查
DELIMITER $$
CREATE PROCEDURE IF NOT EXISTS add_index_if_not_exists()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'T_PROJECT' 
        AND INDEX_NAME = 'IDX_PROJECT_NAME'
    ) THEN
        ALTER TABLE `T_PROJECT` ADD INDEX `IDX_PROJECT_NAME` (`PROJECT_NAME`);
    END IF;
END$$
DELIMITER ;
CALL add_index_if_not_exists();
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
```

**⚠️ 提醒**：添加索引后，别忘了同步更新 `1xxx_*_ddl_mysql.sql` 中的建表语句！

## 禁止操作

1. ❌ **删除后重建表**：可能导致数据丢失
2. ❌ **添加非空无默认值字段**：会导致现有数据插入失败
3. ❌ **直接修改字段类型**：可能导致数据截断
4. ❌ **删除字段**：需要充分评估影响

## 表命名规范

| 前缀 | 含义 | 示例 |
|------|------|------|
| `T_` | 业务表 | `T_PROJECT`, `T_PIPELINE` |
| `T_AUDIT_` | 审计表 | `T_AUDIT_RESOURCE` |
| `T_HISTORY_` | 历史表 | `T_HISTORY_BUILD` |

## 字段命名规范

| 字段 | 类型 | 说明 |
|------|------|------|
| `ID` | bigint(20) | 主键自增 |
| `PROJECT_ID` | varchar(64) | 项目ID |
| `PIPELINE_ID` | varchar(34) | 流水线ID |
| `BUILD_ID` | varchar(34) | 构建ID |
| `CREATE_TIME` | datetime(3) | 创建时间 |
| `UPDATE_TIME` | datetime(3) | 更新时间 |
| `CREATOR` | varchar(64) | 创建人 |
| `MODIFIER` | varchar(64) | 修改人 |

## 索引命名规范

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| 主键 | `PRIMARY` | `PRIMARY KEY (ID)` |
| 唯一索引 | `UNI_{字段}` | `UNIQUE KEY UNI_PROJECT_ID` |
| 普通索引 | `IDX_{字段}` | `INDEX IDX_CREATE_TIME` |
| 联合索引 | `IDX_{字段1}_{字段2}` | `INDEX IDX_PROJECT_PIPELINE` |

## 完整建表示例

```sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `T_PIPELINE_INFO` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `PIPELINE_NAME` varchar(255) NOT NULL COMMENT '流水线名称',
    `PIPELINE_DESC` varchar(1024) DEFAULT NULL COMMENT '流水线描述',
    `VERSION` int(11) NOT NULL DEFAULT '1' COMMENT '版本号',
    `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    `CREATOR` varchar(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER` varchar(64) NOT NULL DEFAULT 'system' COMMENT '修改人',
    `DELETE` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`),
    KEY `IDX_CREATE_TIME` (`CREATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线信息表';

SET FOREIGN_KEY_CHECKS = 1;
```

## JOOQ 集成

BK-CI 使用 JOOQ 作为数据库访问层，禁止手写 SQL：

```kotlin
// ✅ 正确：使用 JOOQ DSL
dslContext.selectFrom(T_PIPELINE_INFO)
    .where(T_PIPELINE_INFO.PROJECT_ID.eq(projectId))
    .and(T_PIPELINE_INFO.DELETE.eq(false))
    .fetch()

// ❌ 错误：手写 SQL
dslContext.fetch("SELECT * FROM T_PIPELINE_INFO WHERE PROJECT_ID = ?", projectId)
```

## 常见错误案例

### ❌ 错误：仅更新增量脚本

**问题场景**：开发者只在增量脚本中添加索引，忘记更新全量 DDL

```sql
-- 文件：2025_v4.x/2040_ci_project-update_v4.1_mysql.sql
ALTER TABLE `T_PROJECT` ADD INDEX `IDX_PROJECT_NAME` (`PROJECT_NAME`);
```

**后果**：
- 旧环境升级 ✅ 成功（执行了增量脚本）
- 全新安装 ❌ 失败（缺少索引，查询性能问题）

**正确做法**：同步更新 `1001_ci_project_ddl_mysql.sql`

---

### ❌ 错误：两处定义不一致

**增量脚本**：
```sql
ALTER TABLE `T_PROJECT` ADD COLUMN `NEW_FIELD` varchar(64) DEFAULT NULL;
```

**全量脚本**：
```sql
`NEW_FIELD` varchar(128) DEFAULT ''  -- ⚠️ 长度和默认值不一致！
```

**后果**：升级环境和全新安装的字段定义不同，可能导致数据截断或业务逻辑错误。

**正确做法**：保持两处完全一致。

---

### ✅ 正确示例：完整的双轨更新

**需求**：为 `T_PIPELINE_INFO` 添加 `TAGS` 字段和索引

**步骤 1：增量脚本**

文件：`2025_v4.x/2040_ci_process-update_v4.1_mysql.sql`

```sql
USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- 添加字段
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND COLUMN_NAME = 'TAGS') THEN
        ALTER TABLE `T_PIPELINE_INFO` 
        ADD COLUMN `TAGS` varchar(1024) DEFAULT NULL COMMENT '流水线标签';
    END IF;

    -- 添加索引
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_INFO'
                    AND INDEX_NAME = 'IDX_TAGS') THEN
        ALTER TABLE `T_PIPELINE_INFO` ADD INDEX `IDX_TAGS` (`TAGS`(255));
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;

CALL ci_process_schema_update();
```

**步骤 2：全量脚本**

文件：`1001_ci_process_ddl_mysql.sql`

```sql
CREATE TABLE IF NOT EXISTS `T_PIPELINE_INFO` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `PIPELINE_NAME` varchar(255) NOT NULL COMMENT '流水线名称',
    `TAGS` varchar(1024) DEFAULT NULL COMMENT '流水线标签',  -- ✅ 新增字段
    -- ... 其他字段
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNI_PROJECT_PIPELINE` (`PROJECT_ID`, `PIPELINE_ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`),
    KEY `IDX_TAGS` (`TAGS`(255))  -- ✅ 新增索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线信息表';
```

**验证**：
```bash
# 测试升级场景
docker exec -it mysql mysql -u root -p devops_ci_process < 2040_ci_process-update_v4.1_mysql.sql

# 测试全新安装
docker exec -it mysql mysql -u root -p devops_ci_process < 1001_ci_process_ddl_mysql.sql

# 对比两种方式的表结构是否一致
docker exec -it mysql mysql -u root -p -e "SHOW CREATE TABLE devops_ci_process.T_PIPELINE_INFO\G"
```

## 数据库分库

BK-CI 支持数据库分库，通过配置可以进行水平扩展：

```yaml
# 分库配置示例
spring:
  datasource:
    process:
      url: jdbc:mysql://host1:3306/devops_ci_process
    project:
      url: jdbc:mysql://host2:3306/devops_ci_project
```
