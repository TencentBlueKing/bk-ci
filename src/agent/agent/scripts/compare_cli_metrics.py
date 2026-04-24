#!/usr/bin/env python3
"""
compare_cli_metrics.py

解析 agentcli monitor 子命令输出的 test.log（包含 collector telegraf 与
monitor gopsutil 两段），按 measurement 对齐两侧的 tag 集合与 field 集合，
打印差异结论，方便判断 out 模式下 rename 是否对齐 telegraf
win_perf_counters 的输出格式。

用法：
    python3 compare_cli_metrics.py bin/test.log
"""

from __future__ import annotations
import re
import sys
from collections import defaultdict
from pathlib import Path


LINE_RE = re.compile(r"^([A-Za-z_][\w]*)(,[^\s]+)?\s+([^\s]+)\s+(\d+)\s*$")
SECTION_COLLECTOR = "collector"
SECTION_MONITOR = "monitor"


def parse_kv(s: str) -> dict[str, str]:
    """解析 line protocol 的 tag/field 段（foo=bar,baz=qux），尊重反斜杠转义。"""
    out: dict[str, str] = {}
    buf, key = [], None
    escape = False
    for ch in s:
        if escape:
            buf.append(ch)
            escape = False
            continue
        if ch == "\\":
            escape = True
            buf.append(ch)
            continue
        if key is None and ch == "=":
            key = "".join(buf)
            buf = []
            continue
        if key is not None and ch == ",":
            out[key] = "".join(buf)
            key, buf = None, []
            continue
        buf.append(ch)
    if key is not None:
        out[key] = "".join(buf)
    return out


def parse_line(line: str) -> tuple[str, dict, dict] | None:
    line = line.strip()
    if not line or line.startswith("#") or line.startswith("===="):
        return None
    m = LINE_RE.match(line)
    if not m:
        return None
    name = m.group(1)
    tags_raw = m.group(2) or ""
    fields_raw = m.group(3)
    tags = parse_kv(tags_raw.lstrip(",")) if tags_raw else {}
    fields = parse_kv(fields_raw)
    return name, tags, fields


def parse_log(path: Path) -> dict[str, dict[str, dict]]:
    """
    返回 {section: {measurement: {"tags": set, "fields": set}}}。
    每个 measurement 聚合该段内所有 metric 的 tag/field 名（并集）。
    """
    data: dict[str, dict[str, dict]] = {
        SECTION_COLLECTOR: defaultdict(lambda: {"tags": set(), "fields": set()}),
        SECTION_MONITOR: defaultdict(lambda: {"tags": set(), "fields": set()}),
    }
    section: str | None = None
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if "collector (telegraf)" in raw:
            section = SECTION_COLLECTOR
            continue
        if "monitor (gopsutil)" in raw:
            section = SECTION_MONITOR
            continue
        if section is None:
            continue
        parsed = parse_line(raw)
        if not parsed:
            continue
        name, tags, fields = parsed
        bucket = data[section][name]
        bucket["tags"].update(tags.keys())
        bucket["fields"].update(fields.keys())
    return data


GLOBAL_TAGS = {
    "agentId", "agentSecret", "cpuProductInfo", "gpuProductInfo",
    "host", "projectId", "hostName", "hostIp",
}


def diff_set(left: set, right: set) -> tuple[set, set, set]:
    return left - right, right - left, left & right


def section_hdr(title: str) -> str:
    bar = "─" * len(title)
    return f"\n{title}\n{bar}"


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"[err] not a file: {path}", file=sys.stderr)
        return 2

    data = parse_log(path)
    col = data[SECTION_COLLECTOR]
    mon = data[SECTION_MONITOR]

    all_measurements = sorted(set(col) | set(mon))
    col_only = sorted(set(col) - set(mon))
    mon_only = sorted(set(mon) - set(col))
    both = sorted(set(col) & set(mon))

    print(section_hdr("Measurement 对齐情况"))
    print(f"  两侧都有:  {len(both):>2} 个  →  {', '.join(both)}")
    if col_only:
        print(f"  仅 collector: {', '.join(col_only)}")
    if mon_only:
        print(f"  仅 monitor:   {', '.join(mon_only)}")

    # 按 measurement 做 tag/field 并集对比
    print(section_hdr("每个 measurement 的 tag/field 差异（忽略 global tags）"))
    for m in all_measurements:
        c = col.get(m, {"tags": set(), "fields": set()})
        o = mon.get(m, {"tags": set(), "fields": set()})
        c_tags = c["tags"] - GLOBAL_TAGS
        o_tags = o["tags"] - GLOBAL_TAGS
        c_fields = c["fields"]
        o_fields = o["fields"]

        t_col_only, t_mon_only, t_both = diff_set(c_tags, o_tags)
        f_col_only, f_mon_only, f_both = diff_set(c_fields, o_fields)

        side = []
        if m in col and m not in mon:
            side.append("仅 collector")
        elif m in mon and m not in col:
            side.append("仅 monitor")
        status = ""
        if not (t_col_only or t_mon_only or f_col_only or f_mon_only) and m in col and m in mon:
            status = "  [OK] 完全对齐"
        print(f"\n  [{m}]{' (' + ','.join(side) + ')' if side else ''}{status}")
        if t_both or t_col_only or t_mon_only:
            print(f"    tags  共有: {sorted(t_both) or '-'}")
            if t_col_only:
                print(f"    tags  仅 collector: {sorted(t_col_only)}")
            if t_mon_only:
                print(f"    tags  仅 monitor:   {sorted(t_mon_only)}")
        if f_col_only:
            print(f"    fields 仅 collector: {sorted(f_col_only)}")
        if f_mon_only:
            print(f"    fields 仅 monitor:   {sorted(f_mon_only)}")
        if f_both and not (f_col_only or f_mon_only):
            print(f"    fields 共有 ({len(f_both)} 项): {sorted(f_both)}")
        elif f_both:
            # 同时有两侧独有和共有时，共有只给个计数
            print(f"    fields 共有 {len(f_both)} 项")

    # 摘要
    print(section_hdr("结论摘要"))
    total_col_only = sum(
        1 for m in both
        if (col[m]["fields"] - mon[m]["fields"]) or ((col[m]["tags"] - GLOBAL_TAGS) - (mon[m]["tags"] - GLOBAL_TAGS))
    )
    total_mon_only = sum(
        1 for m in both
        if (mon[m]["fields"] - col[m]["fields"]) or ((mon[m]["tags"] - GLOBAL_TAGS) - (col[m]["tags"] - GLOBAL_TAGS))
    )
    print(f"  measurement 两侧都存在: {len(both)} 个，其中")
    print(f"    collector 侧有 monitor 没有的 tag/field: {total_col_only} 个")
    print(f"    monitor 侧有 collector 没有的 tag/field: {total_mon_only} 个")
    if col_only:
        print(f"  collector 独有的 measurement: {col_only}")
    if mon_only:
        print(f"  monitor 独有的 measurement:   {mon_only}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
