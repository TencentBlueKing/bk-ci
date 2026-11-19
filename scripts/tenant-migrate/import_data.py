#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
蓝盾 CI 数据导入脚本
将 export_data.py 导出的数据导入到新的多租户数据库中，自动填充 tenant_id 等多租户字段。

使用方式:
    python import_data.py \
        --host 10.0.0.2 \
        --port 3306 \
        --user root \
        --password 'xxx' \
        --input-dir ./export_data

说明:
    1. 新数据库必须已经通过 DDL 脚本初始化好表结构（含多租户字段）
    2. 脚本会读取导出的 SQL 文件，解析数据，补充多租户字段后写入新库
    3. devops_ci_store 下的 tenant_id 填充为 "default"（平台默认租户）
    4. 仅 T_PROJECT / T_PROJECT_APPROVAL 填充为 "tencent"（迁移源租户，不是平台默认值）
    5. 未出现在映射里的表不改写 tenant_id，沿用 DDL 默认值
    6. T_PROJECT / T_PROJECT_APPROVAL 的 tenant_english_name 用 english_name 填充
    7. 使用 INSERT IGNORE 避免重复数据冲突，并按实际写入行数和告警统计
    8. --skip-existing 不再整表跳过；表里已有数据时继续导入，重复行由 INSERT IGNORE 跳过
"""

import argparse
import os
import re
import sys
import time
import pymysql

# 所有需要迁移的数据库
ALL_DATABASES = [
    "devops_ci_project",
    "devops_ci_store",
    "devops_ci_process",
    "devops_ci_environment",
    "devops_ci_auth",
    "devops_ci_repository",
    "devops_ci_artifactory",
    "devops_ci_dispatch",
    "devops_ci_quality",
    "devops_ci_notify",
    "devops_ci_ticket",
    "devops_ci_plugin",
    "devops_ci_log",
    "devops_ci_image",
    "devops_ci_metrics",
    "devops_ci_op",
    "devops_ci_openapi",
    "devops_ci_stream",
    "devops_ci_dispatch_kubernetes",
    "devops_ci_archive_process",
]

# ========================================================================
# 多租户字段映射配置
# key: (database, table)
# value: dict of { 新增字段名: 填充规则 }
#   - 字符串值: 固定填充该值
#   - 以 "$" 开头: 引用同行的其他字段值（从导出数据的列中取）
# ========================================================================
TENANT_FIELD_MAP = {
    ("devops_ci_project", "T_PROJECT"): {
        "tenant_id": "tencent",
        "tenant_english_name": "$english_name",
    },
    ("devops_ci_project", "T_PROJECT_APPROVAL"): {
        "TENANT_ID": "tencent",
        "TENANT_ENGLISH_NAME": "$ENGLISH_NAME",
    },
    ("devops_ci_store", "T_ATOM"): {
        "TENANT_ID": "default",
    },
    ("devops_ci_store", "T_TEMPLATE"): {
        "TENANT_ID": "default",
    },
    ("devops_ci_store", "T_IMAGE"): {
        "TENANT_ID": "default",
    },
}

BATCH_SIZE = 2000

# 正则: 匹配导出文件中的 INSERT 语句头部
RE_INSERT_HEAD = re.compile(
    r"INSERT\s+INTO\s+`(\w+)`\s*\(([^)]+)\)\s*VALUES\s*", re.IGNORECASE
)


def get_connection(host, port, user, password, database=None):
    return pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.Cursor,
        autocommit=False,
    )


def execute_session_setting(conn, sql, warn_prefix):
    """执行会话级 SET 语句；权限不足时仅告警并跳过。"""
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
    except pymysql.err.OperationalError as e:
        if e.args and e.args[0] == 1227:
            print(f"{warn_prefix}{sql}: {e}")
            return False
        raise
    return True


def print_sql_error(prefix, error):
    """只打印定位信息和错误，不输出 SQL 正文。"""
    print(f"{prefix}: {error}")


def find_statement_end(text, start):
    """在字符串外查找语句结束分号。引号内的分号不截断。"""
    in_string = False
    quote_char = None
    i = start
    while i < len(text):
        ch = text[i]
        if in_string:
            if ch == "\\" and i + 1 < len(text):
                i += 2
                continue
            if ch == quote_char:
                if i + 1 < len(text) and text[i + 1] == quote_char:
                    i += 2
                    continue
                in_string = False
                quote_char = None
            i += 1
            continue
        if ch in ("'", '"'):
            in_string = True
            quote_char = ch
        elif ch == ";":
            return i
        i += 1
    return -1


def iter_insert_statements(filepath):
    """
    按 INSERT 语句流式产出 (table, columns, values_str)。
    只保留当前语句，语句结束分号按引号感知定位。
    """
    buf = ""
    with open(filepath, "r", encoding="utf-8") as f:
        while True:
            chunk = f.read(1024 * 1024)
            if chunk:
                buf += chunk
            while True:
                match = RE_INSERT_HEAD.search(buf)
                if not match:
                    break
                end = find_statement_end(buf, match.end())
                if end == -1:
                    buf = buf[match.start():]
                    break
                columns = [c.strip().strip("`") for c in match.group(2).split(",")]
                yield match.group(1), columns, buf[match.end():end]
                buf = buf[end + 1:]
            if not chunk:
                if RE_INSERT_HEAD.search(buf):
                    print(f"    [WARN] {filepath}: 末尾 INSERT 未闭合，已丢弃剩余片段")
                break
            if not RE_INSERT_HEAD.search(buf) and len(buf) > 8192:
                buf = buf[-4096:]


def get_target_columns(conn, database, table):
    """获取目标库中表的列名列表"""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
            "WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s "
            "ORDER BY ORDINAL_POSITION",
            (database, table),
        )
        return [row[0] for row in cur.fetchall()]


def get_row_count(conn, table):
    """获取表行数"""
    with conn.cursor() as cur:
        cur.execute(f"SELECT COUNT(*) FROM `{table}`")
        return cur.fetchone()[0]


def parse_insert_statements(filepath):
    """兼容旧调用，逐条产出 INSERT。"""
    yield from iter_insert_statements(filepath)


def parse_row_values(values_str):
    """线性扫描 values 字符串，提取每一行的原始字面量字符串（含外层括号）。"""
    row_literals = []
    in_string = False
    quote_char = None
    depth = 0
    row_start = None
    i = 0

    while i < len(values_str):
        ch = values_str[i]

        if in_string:
            if ch == "\\" and i + 1 < len(values_str):
                i += 2
                continue
            if ch == quote_char:
                if i + 1 < len(values_str) and values_str[i + 1] == quote_char:
                    i += 2
                    continue
                in_string = False
                quote_char = None
            i += 1
            continue

        if ch in ("'", '"'):
            in_string = True
            quote_char = ch
        elif ch == "(":
            if depth == 0:
                row_start = i
            depth += 1
        elif ch == ")" and depth > 0:
            depth -= 1
            if depth == 0 and row_start is not None:
                row_literals.append(values_str[row_start:i + 1])
                row_start = None
        i += 1

    return row_literals


def resolve_field_value(rule, col_index_map, all_vals):
    """
    根据填充规则获取字段值的 SQL 字面量。
    - rule 是固定字符串 -> 返回 'value'
    - rule 以 "$" 开头 -> 从该行已拆好的列值中取值
    """
    if rule.startswith("$"):
        ref_col = rule[1:]
        idx = col_index_map.get(ref_col)
        if idx is None:
            ref_lower = ref_col.lower()
            for col_name, col_idx in col_index_map.items():
                if col_name.lower() == ref_lower:
                    idx = col_idx
                    break
        if idx is None or idx >= len(all_vals):
            return "NULL"
        return all_vals[idx].strip()
    return f"'{rule}'"


def extract_nth_value(row_literal, n):
    """
    从 (val1, val2, ..., valN) 形式的字符串中提取第 n 个值的原始 SQL 字面量。
    需要正确处理字符串中的逗号和括号。
    """
    # 去掉外层括号
    inner = row_literal.strip()
    if inner.startswith("("):
        inner = inner[1:]
    if inner.endswith(")"):
        inner = inner[:-1]

    values = split_sql_values(inner)
    if n < len(values):
        return values[n].strip()
    return "NULL"


def split_sql_values(s):
    """
    按顶层逗号分割 SQL 值列表，正确处理字符串引号内的逗号和转义。
    """
    result = []
    current = []
    in_string = False
    quote_char = None
    i = 0
    depth = 0  # 括号嵌套深度

    while i < len(s):
        ch = s[i]

        if in_string:
            current.append(ch)
            if ch == "\\" and i + 1 < len(s):
                # 转义字符，连下一个字符一起吃掉
                i += 1
                current.append(s[i])
            elif ch == quote_char:
                # 检查是否是双引号转义 '' 或 ""
                if i + 1 < len(s) and s[i + 1] == quote_char:
                    current.append(s[i + 1])
                    i += 1
                else:
                    in_string = False
                    quote_char = None
        else:
            if ch in ("'", '"'):
                in_string = True
                quote_char = ch
                current.append(ch)
            elif ch == "(":
                depth += 1
                current.append(ch)
            elif ch == ")":
                depth -= 1
                current.append(ch)
            elif ch == "," and depth == 0:
                result.append("".join(current))
                current = []
            else:
                current.append(ch)
        i += 1

    if current:
        result.append("".join(current))
    return result


def import_table_file(conn, database, table_file, tenant_fields, skip_existing):
    """
    导入单个表文件到目标库。

    参数:
        conn: 目标库连接
        database: 数据库名
        table_file: 导出的 .sql 文件路径
        tenant_fields: 该表需要补充的多租户字段 dict 或 None
        skip_existing: 是否跳过已有数据的表
    返回:
        导入行数
    """
    table_name = os.path.splitext(os.path.basename(table_file))[0]

    # 检查目标表是否存在
    target_columns = get_target_columns(conn, database, table_name)
    if not target_columns:
        print(f"    [SKIP] {table_name}: 目标库中不存在此表")
        return 0

    if skip_existing:
        existing_count = get_row_count(conn, table_name)
        if existing_count > 0:
            print(
                f"    [INFO] {table_name}: 目标表已有 {existing_count} 行，"
                "继续导入，重复行由 INSERT IGNORE 跳过"
            )

    target_col_set = {c.lower() for c in target_columns}
    total_imported = 0

    for table, src_columns, values_str in parse_insert_statements(table_file):
        col_index_map = {col: idx for idx, col in enumerate(src_columns)}
        row_literals = parse_row_values(values_str)

        if not row_literals:
            continue

        # 确定要写入哪些列: 源列中在目标表存在的 + 需要补充的多租户列
        write_columns = []
        src_col_indices = []
        for idx, col in enumerate(src_columns):
            if col.lower() in target_col_set:
                write_columns.append(col)
                src_col_indices.append(idx)

        # 需要补充的多租户字段（源数据中不存在、目标表中存在的）
        extra_fields = {}
        if tenant_fields:
            src_col_lower_set = {c.lower() for c in src_columns}
            for field_name, rule in tenant_fields.items():
                if field_name.lower() not in src_col_lower_set and field_name.lower() in target_col_set:
                    extra_fields[field_name] = rule

        all_write_columns = write_columns + list(extra_fields.keys())
        col_names_sql = ", ".join(f"`{c}`" for c in all_write_columns)

        # 分批导入
        for batch_start in range(0, len(row_literals), BATCH_SIZE):
            batch = row_literals[batch_start: batch_start + BATCH_SIZE]
            new_rows = []
            for row_lit in batch:
                # 提取源列中需要的值
                all_vals = split_sql_values(
                    row_lit.strip()[1:-1]  # 去掉外层括号
                )
                picked = []
                for idx in src_col_indices:
                    if idx < len(all_vals):
                        picked.append(all_vals[idx].strip())
                    else:
                        picked.append("NULL")

                # 补充多租户字段值
                for field_name, rule in extra_fields.items():
                    picked.append(resolve_field_value(rule, col_index_map, all_vals))

                new_rows.append("(" + ", ".join(picked) + ")")

            sql = (
                f"INSERT IGNORE INTO `{table_name}` ({col_names_sql}) VALUES\n"
                + ",\n".join(new_rows)
                + ";"
            )
            try:
                with conn.cursor() as cur:
                    cur.execute(sql)
                    inserted = cur.rowcount if cur.rowcount and cur.rowcount > 0 else 0
                    cur.execute("SHOW WARNINGS")
                    warnings = cur.fetchall()
                conn.commit()
                total_imported += inserted
                real_warnings = [w for w in warnings if len(w) < 2 or w[1] != 1062]
                if real_warnings:
                    print(
                        f"    [WARN] {table_name} batch@{batch_start}: "
                        f"写入 {inserted} 行，告警 {len(real_warnings)} 条"
                    )
                    for warning in real_warnings[:5]:
                        print(f"        {warning}")
                elif inserted < len(batch):
                    print(
                        f"    [INFO] {table_name} batch@{batch_start}: "
                        f"写入 {inserted} 行，重复跳过 {len(batch) - inserted} 行"
                    )
            except Exception as e:
                conn.rollback()
                print_sql_error(
                    f"    [ERROR] {table_name} batch@{batch_start}",
                    e,
                )
                for row_index, row_sql in enumerate(new_rows):
                    single = (
                        f"INSERT IGNORE INTO `{table_name}` ({col_names_sql}) "
                        f"VALUES\n{row_sql};"
                    )
                    try:
                        with conn.cursor() as cur:
                            cur.execute(single)
                            inserted = cur.rowcount if cur.rowcount and cur.rowcount > 0 else 0
                            cur.execute("SHOW WARNINGS")
                            warnings = cur.fetchall()
                        conn.commit()
                        total_imported += inserted
                        real_warnings = [w for w in warnings if len(w) < 2 or w[1] != 1062]
                        if real_warnings:
                            print(
                                f"    [WARN] {table_name} row@{batch_start + row_index}: "
                                f"{real_warnings[0]}"
                            )
                    except Exception as e2:
                        conn.rollback()
                        print_sql_error(
                            f"    [ERROR] {table_name} row@{batch_start + row_index}",
                            e2,
                        )

    return total_imported


def import_database(host, port, user, password, database, input_dir, skip_existing):
    """导入单个数据库的所有表"""
    db_dir = os.path.join(input_dir, database)
    if not os.path.isdir(db_dir):
        print(f"\n[SKIP] {database}: 导出目录不存在 {db_dir}")
        return

    sql_files = sorted(
        [f for f in os.listdir(db_dir) if f.endswith(".sql")]
    )
    if not sql_files:
        print(f"\n[SKIP] {database}: 无 SQL 文件")
        return

    print(f"\n[{database}] 共 {len(sql_files)} 张表待导入")

    conn = get_connection(host, port, user, password, database)
    try:
        # 关闭外键检查和唯一键检查以加速导入
        execute_session_setting(conn, "SET FOREIGN_KEY_CHECKS = 0", "    [WARN] 跳过会话设置: ")
        execute_session_setting(conn, "SET UNIQUE_CHECKS = 0", "    [WARN] 跳过会话设置: ")
        conn.commit()

        for sql_file in sql_files:
            table_name = os.path.splitext(sql_file)[0]
            table_file = os.path.join(db_dir, sql_file)
            tenant_key = (database, table_name)
            tenant_fields = TENANT_FIELD_MAP.get(tenant_key)

            count = import_table_file(
                conn, database, table_file, tenant_fields, skip_existing
            )
            tag = " [+tenant]" if tenant_fields else ""
            print(f"  {table_name}: {count} 行{tag}")

        # 恢复设置
        execute_session_setting(conn, "SET FOREIGN_KEY_CHECKS = 1", "    [WARN] 恢复会话设置失败: ")
        execute_session_setting(conn, "SET UNIQUE_CHECKS = 1", "    [WARN] 恢复会话设置失败: ")
        conn.commit()
    finally:
        conn.close()


def main():
    parser = argparse.ArgumentParser(
        description="蓝盾 CI 数据导入工具 (非多租户 -> 多租户迁移)"
    )
    parser.add_argument("--host", required=True, help="目标 MySQL 地址")
    parser.add_argument("--port", type=int, default=3306, help="目标 MySQL 端口")
    parser.add_argument("--user", required=True, help="MySQL 用户名")
    parser.add_argument("--password", required=True, help="MySQL 密码")
    parser.add_argument("--input-dir", default="./export_data", help="导入数据目录")
    parser.add_argument(
        "--databases",
        nargs="*",
        default=None,
        help="指定要导入的数据库列表，默认导入全部",
    )
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        default=False,
        help="跳过目标表中已有数据的表",
    )
    args = parser.parse_args()

    databases = args.databases if args.databases else ALL_DATABASES

    # 检查连接
    try:
        conn = get_connection(args.host, args.port, args.user, args.password)
        conn.close()
    except Exception as e:
        print(f"[ERROR] 无法连接到 MySQL {args.host}:{args.port} - {e}")
        sys.exit(1)

    if not os.path.isdir(args.input_dir):
        print(f"[ERROR] 导入目录不存在: {args.input_dir}")
        sys.exit(1)

    print(f"导入数据目录: {os.path.abspath(args.input_dir)}")
    print(f"待导入数据库: {len(databases)} 个")
    print(f"多租户字段填充规则:")
    print(f"  devops_ci_store 下 tenant_id = 'default'")
    print(f"  T_PROJECT / T_PROJECT_APPROVAL tenant_id = 'tencent'")
    print(f"  未配置的表沿用 DDL 默认值")
    print()

    total_start = time.time()
    for database in databases:
        try:
            import_database(
                args.host,
                args.port,
                args.user,
                args.password,
                database,
                args.input_dir,
                args.skip_existing,
            )
        except Exception as e:
            print(f"[ERROR] 导入 {database} 失败: {e}")
            continue

    elapsed = time.time() - total_start
    print(f"\n导入完成, 耗时 {elapsed:.1f}s")


if __name__ == "__main__":
    main()
