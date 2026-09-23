# 脚本说明

## 第一步：导出 export_data.py

从旧的非多租户 MySQL 导出全部 20 个数据库的所有表数据：

```bash
python export_data.py \
    --host 10.0.0.1 \
    --port 3306 \
    --user root \
    --password 'xxx' \
    --output-dir ./export_data
```

- 每张表导出为一个 SQL 文件：`./export_data/{database}/{table}.sql`
- 支持 `--databases` 参数指定只导出某些库
- 分批 5000 行导出，避免内存溢出

## 第二步：导入 import_data.py

将导出数据导入到新的多租户 MySQL，自动补充多租户字段：

```bash
python import_data.py \
    --host 10.0.0.2 \
    --port 3306 \
    --user root \
    --password 'xxx' \
    --input-dir ./export_data
```

### 多租户字段自动填充逻辑

| 数据库 | 表 | 填充字段 | 填充值 |
| --- | --- | --- | --- |
| devops_ci_store | T_ATOM | TENANT_ID | system |
| devops_ci_store | T_TEMPLATE | TENANT_ID | system |
| devops_ci_store | T_IMAGE | TENANT_ID | system |
| devops_ci_project | T_PROJECT | tenant_id | tencent |
| devops_ci_project | T_PROJECT | tenant_english_name | 取自该行 english_name 值 |
| devops_ci_project | T_PROJECT_APPROVAL | TENANT_ID | tencent |
| devops_ci_project | T_PROJECT_APPROVAL | TENANT_ENGLISH_NAME | 取自该行 ENGLISH_NAME 值 |

### 关键特性

- **幂等**：导入使用 `INSERT IGNORE`，重复执行不会报错
- **断点续跑**：`--skip-existing` 跳过目标表已有数据的表
- **容错**：批量插入失败时自动逐行重试，定位问题行
- **安全**：导入前关闭外键检查加速，完成后恢复
- **源列兼容**：自动对比源数据列与目标表列，跳过目标表不存在的列，新增的多租户列自动补充

## 依赖

```bash
pip install pymysql
```
