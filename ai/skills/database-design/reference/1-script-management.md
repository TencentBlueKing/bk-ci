# 数据库脚本管理规范

# 数据库脚本管理

## Quick Reference

```
数据库：MySQL 5.7.2+ / MariaDB 10.x+
SQL 位置：support-files/sql/
全量 DDL：1xxx_{系统}_{模块}_ddl_mysql.sql
增量更新：2xxx_{系统}_{模块}_update_{版本}_mysql.sql

⚠️ 核心原则：双轨更新 - 修改表结构必须同时更新增量脚本和全量 DDL！
```

### 最简示例（添加索引）

**步骤 1：增量脚本** `support-files/sql/2025_v4.x/2040_ci_project-update_v4.1_mysql.sql`

```sql
USE devops_ci_project;
DROP PROCEDURE IF EXISTS ci_project_schema_update;
DELIMITER <CI_UBF>
CREATE PROCEDURE ci_project_schema_update()
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'T_PROJECT'
                    AND INDEX_NAME = 'IDX_PROJECT_NAME') THEN
        ALTER TABLE `T_PROJECT` ADD INDEX `IDX_PROJECT_NAME` (`PROJECT_NAME`);
    END IF;
END <CI_UBF>
DELIMITER ;
CALL ci_project_schema_update();
```

**步骤 2：全量 DDL** `support-files/sql/1001_ci_project_ddl_mysql.sql`

```sql
CREATE TABLE IF NOT EXISTS `T_PROJECT` (
    ...
    KEY `IDX_PROJECT_NAME` (`PROJECT_NAME`)  -- ✅ 同步添加
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## When to Use

- 新增数据库表
- 添加/修改字段、索引
- 编写数据迁移脚本
- 管理 SQL 版本

## When NOT to Use

- JOOQ 代码编写 → 参考 `process-module-architecture`（reference/4-dao-database.md）
- 表结构设计规范 → 使用 `44-database-design`

---

## 文件命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 创建数据库 | `0001_ci_create-database_mysql.sql` | - |
| 全量 DDL | `1xxx_ci_{模块}_ddl_mysql.sql` | `1001_ci_project_ddl_mysql.sql` |
| 增量更新 | `2xxx_ci_{模块}_update_{版本}_mysql.sql` | `2003_ci_process_update_v2.0.0_mysql.sql` |
| 初始化数据 | `5001_ci_{模块}_dml_mysql.sql` | - |

## 双轨更新机制（⚠️ 重要）

**任何表结构变更必须同时更新两处：**

1. **增量脚本**（`2xxx_*_update_*.sql`）- 用于现有环境升级
2. **全量 DDL**（`1xxx_*_ddl_*.sql`）- 用于全新安装

| 变更类型 | 增量脚本操作 | 全量脚本操作 |
|----------|-------------|-------------|
| 添加列 | `ALTER TABLE ADD COLUMN` | 在 CREATE TABLE 中添加字段 |
| 添加索引 | `ALTER TABLE ADD INDEX` | 在 CREATE TABLE 的 KEY 区域添加 |
| 修改列 | `ALTER TABLE MODIFY COLUMN` | 修改 CREATE TABLE 中的字段定义 |

## 幂等性要求

```sql
-- ✅ 创建表：使用 IF NOT EXISTS
CREATE TABLE IF NOT EXISTS `T_PROJECT` (...);

-- ✅ 添加列：使用存储过程检查
IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS 
               WHERE TABLE_NAME = 'T_PROJECT' AND COLUMN_NAME = 'NEW_COL') THEN
    ALTER TABLE `T_PROJECT` ADD COLUMN `NEW_COL` varchar(64);
END IF;

-- ✅ 插入数据：使用 INSERT IGNORE
INSERT IGNORE INTO `T_CONFIG` (`KEY`, `VALUE`) VALUES ('key', 'value');
```

## 命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 表名 | `T_{业务}` | `T_PROJECT`, `T_PIPELINE_INFO` |
| 主键 | `PRIMARY` | `PRIMARY KEY (ID)` |
| 唯一索引 | `UNI_{字段}` | `UNIQUE KEY UNI_PROJECT_ID` |
| 普通索引 | `IDX_{字段}` | `INDEX IDX_CREATE_TIME` |

## 禁止操作

- ❌ 删除后重建表（可能导致数据丢失）
- ❌ 添加非空无默认值字段
- ❌ 直接修改字段类型（可能数据截断）
- ❌ 仅更新增量脚本，不更新全量 DDL

---

## Checklist

提交数据库变更前确认：
- [ ] 增量脚本已创建
- [ ] 增量脚本具备幂等性
- [ ] **全量 DDL 已同步更新**
- [ ] 两处定义完全一致（类型、默认值、索引名）
- [ ] 本地测试通过（全新安装 + 升级安装）
