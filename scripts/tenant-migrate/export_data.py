#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
蓝盾 CI 数据导出脚本
从旧的非多租户 MySQL 数据库导出所有库表数据为 SQL 文件，供导入多租户新库使用。

使用方式:
    python export_data.py \
        --host 10.0.0.1 \
        --port 3306 \
        --user root \
        --password 'xxx' \
        --output-dir ./export_data

输出:
    ./export_data/{database}/{table}.sql  每张表一个文件
"""

import argparse
import os
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

BATCH_SIZE = 5000


def get_connection(host, port, user, password, database=None):
    return pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.Cursor,
    )


def get_all_tables(conn, database):
    """获取指定数据库下所有表名"""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT TABLE_NAME FROM information_schema.TABLES "
            "WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE' "
            "ORDER BY TABLE_NAME",
            (database,),
        )
        return [row[0] for row in cur.fetchall()]


def get_columns(conn, database, table):
    """获取表的列名列表"""
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


def escape_value(val):
    """将 Python 值转为 SQL 字面量"""
    if val is None:
        return "NULL"
    if isinstance(val, bool):
        return "1" if val else "0"
    if isinstance(val, (int, float)):
        return str(val)
    if isinstance(val, bytes):
        if len(val) == 0:
            return "X''"
        return "X'" + val.hex() + "'"
    if isinstance(val, bytearray):
        if len(val) == 0:
            return "X''"
        return "X'" + val.hex() + "'"
    # str / datetime / date / decimal 等
    s = str(val)
    s = s.replace("\\", "\\\\").replace("'", "\\'")
    return f"'{s}'"


def export_table(conn, database, table, columns, output_file):
    """导出单张表到 SQL 文件"""
    row_count = get_row_count(conn, table)
    if row_count == 0:
        print(f"  {table}: 0 行，跳过导出")
        return None

    col_names = ", ".join(f"`{c}`" for c in columns)
    written = 0

    with open(output_file, "w", encoding="utf-8") as f:
        f.write(f"-- Table: {database}.{table}\n")
        f.write(f"-- Rows: {row_count}\n")
        f.write(f"-- Exported at: {time.strftime('%Y-%m-%d %H:%M:%S')}\n\n")

        with conn.cursor() as cur:
            cur.execute(f"SELECT {col_names} FROM `{table}`")
            while True:
                rows = cur.fetchmany(BATCH_SIZE)
                if not rows:
                    break
                values_list = []
                for row in rows:
                    vals = ", ".join(escape_value(v) for v in row)
                    values_list.append(f"({vals})")
                f.write(
                    f"INSERT INTO `{table}` ({col_names}) VALUES\n"
                )
                f.write(",\n".join(values_list))
                f.write(";\n\n")
                written += len(rows)

    return written


def export_database(host, port, user, password, database, output_dir):
    """导出单个数据库的所有表"""
    db_dir = os.path.join(output_dir, database)
    os.makedirs(db_dir, exist_ok=True)

    conn = get_connection(host, port, user, password, database)
    try:
        tables = get_all_tables(conn, database)
        print(f"\n[{database}] 共 {len(tables)} 张表")

        for table in tables:
            columns = get_columns(conn, database, table)
            output_file = os.path.join(db_dir, f"{table}.sql")
            count = export_table(conn, database, table, columns, output_file)
            if count is None:
                continue
            print(f"  {table}: {count} 行 -> {output_file}")
    finally:
        conn.close()


def main():
    parser = argparse.ArgumentParser(description="蓝盾 CI 数据导出工具 (非多租户 -> 多租户迁移)")
    parser.add_argument("--host", required=True, help="源 MySQL 地址")
    parser.add_argument("--port", type=int, default=3306, help="源 MySQL 端口")
    parser.add_argument("--user", required=True, help="MySQL 用户名")
    parser.add_argument("--password", required=True, help="MySQL 密码")
    parser.add_argument("--output-dir", default="./export_data", help="导出目录")
    parser.add_argument(
        "--databases",
        nargs="*",
        default=None,
        help="指定要导出的数据库列表，默认导出全部",
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

    os.makedirs(args.output_dir, exist_ok=True)
    print(f"导出目录: {os.path.abspath(args.output_dir)}")
    print(f"待导出数据库: {len(databases)} 个")

    total_start = time.time()
    for database in databases:
        try:
            export_database(
                args.host, args.port, args.user, args.password, database, args.output_dir
            )
        except Exception as e:
            print(f"[ERROR] 导出 {database} 失败: {e}")
            continue

    elapsed = time.time() - total_start
    print(f"\n导出完成, 耗时 {elapsed:.1f}s, 文件位于: {os.path.abspath(args.output_dir)}")


if __name__ == "__main__":
    main()
