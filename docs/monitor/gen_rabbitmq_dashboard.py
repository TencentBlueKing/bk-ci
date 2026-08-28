#!/usr/bin/env python3
"""Generate BKMonitor Grafana JSON for BK-CI prod RabbitMQ cluster."""

import json

DS = {"type": "bkmonitor-timeseries-datasource", "uid": "000000006"}
TABLE = "script_devops_rabbitmq_queue_data.__default__"
LABEL = "script_devops_rabbitmq_queue_data"
COLLECT = "1131"
OUT = "/Users/royalhuang/devops/dev/bk-ci-ext/docs/monitor/grafana-bkci-prod-rabbitmq-mOnpn8q4z.json"

QUEUE_METRICS = [
    ("queueRbacCoreConsumerUtilisationValue", "rbac-core"),
    ("queueRbacExtConsumerUtilisationValue", "rbac-ext"),
    ("queueRbacGrayCoreConsumerUtilisationValue", "rbac-gray-core"),
    ("queueRbacGrayExtConsumerUtilisationValue", "rbac-gray-ext"),
    ("queueAutoCoreConsumerUtilisationValue", "auto-core"),
    ("queueAutoExtCoreConsumerUtilisationValue", "auto-ext"),
    ("queueStreamCoreConsumerUtilisationValue", "stream-core"),
    ("queueStreamExtConsumerUtilisationValue", "stream-ext"),
    ("queueStreamGrayCoreConsumerUtilisationValue", "stream-gray-core"),
    ("queueStreamGrayExtConsumerUtilisationValue", "stream-gray-ext"),
]


def qc(
    metric_field,
    group_by,
    alias,
    method="MAX",
    table=TABLE,
    data_label=LABEL,
    result_table_label="application_check",
    where=None,
    extra=None,
):
    cfg = {
        "alias": alias,
        "data_source_label": "bk_monitor",
        "data_type_label": "time_series",
        "display": True,
        "filter_dict": {},
        "functions": [{"id": "time_shift", "params": [{"id": "n"}]}],
        "group_by": group_by,
        "interval": "auto",
        "interval_unit": "s",
        "method": method,
        "metric_field": metric_field,
        "refId": "a",
        "result_table_id": table,
        "result_table_label": result_table_label,
        "time_field": "time",
        "where": where or [],
    }
    if data_label:
        cfg["data_label"] = data_label
    if extra:
        cfg.update(extra)
    return cfg


def qc_top(metric_field, group_by, alias, n=30, method="MAX", where=None):
    return qc(
        metric_field,
        group_by,
        alias,
        method=method,
        where=where,
        extra={
            "functions": [
                {"id": "time_shift", "params": [{"id": "n"}]},
                {"id": "top", "params": [{"id": "n", "value": str(n)}]},
            ]
        },
    )


def script_where(extra=None):
    w = [
        {"condition": "and", "key": "bk_collect_config_id", "method": "eq", "value": [COLLECT]},
        {"condition": "and", "key": "bk_target_ip", "method": "eq", "value": ["$cluster_ip"]},
    ]
    # cluster_ip 变量本身已限制为 rbac 三节点；All 时 Grafana 会展开这三台。
    if extra:
        w.extend(extra)
    return w


def host_where():
    return [{"condition": "and", "key": "bk_target_ip", "method": "eq", "value": ["$cluster_ip"]}]


def target(query_configs, ref="A"):
    return {
        "datasource": DS,
        "mode": "ui",
        "query_configs": query_configs,
        "refId": ref,
        "source": "",
    }


def ts_custom():
    return {
        "axisBorderShow": False,
        "axisCenteredZero": False,
        "axisColorMode": "text",
        "axisLabel": "",
        "axisPlacement": "auto",
        "barAlignment": 0,
        "drawStyle": "line",
        "fillOpacity": 10,
        "gradientMode": "none",
        "hideFrom": {"legend": False, "tooltip": False, "viz": False},
        "insertNulls": False,
        "lineInterpolation": "linear",
        "lineWidth": 2,
        "pointSize": 5,
        "scaleDistribution": {"type": "linear"},
        "showPoints": "auto",
        "spanNulls": True,
        "stacking": {"group": "A", "mode": "none"},
        "thresholdsStyle": {"mode": "off"},
    }


def panel_base(pid, title, ptype, x, y, w, h):
    return {
        "id": pid,
        "title": title,
        "type": ptype,
        "datasource": DS,
        "gridPos": {"h": h, "w": w, "x": x, "y": y},
        "pluginVersion": "10.0.0",
    }


def timeseries(pid, title, x, y, w, h, targets, unit=None, max_val=None, thresholds=None, threshold_line=False, desc=""):
    p = panel_base(pid, title, "timeseries", x, y, w, h)
    steps = thresholds or [{"color": "green", "value": None}]
    custom = ts_custom()
    if threshold_line:
        custom["thresholdsStyle"] = {"mode": "line+area"}
    p["fieldConfig"] = {
        "defaults": {
            "color": {"mode": "palette-classic"},
            "custom": custom,
            "mappings": [],
            "thresholds": {"mode": "absolute", "steps": steps},
            "unit": unit or "none",
        },
        "overrides": [],
    }
    if max_val is not None:
        p["fieldConfig"]["defaults"]["max"] = max_val
        p["fieldConfig"]["defaults"]["min"] = 0
    p["options"] = {
        "legend": {"calcs": ["lastNotNull", "max"], "displayMode": "table", "placement": "bottom", "showLegend": True},
        "tooltip": {"mode": "multi", "sort": "desc"},
    }
    p["targets"] = targets
    if desc:
        p["description"] = desc
    return p


def stat(pid, title, x, y, w, h, targets, unit=None, thresholds=None, desc=""):
    p = panel_base(pid, title, "stat", x, y, w, h)
    steps = thresholds or [{"color": "green", "value": None}]
    p["fieldConfig"] = {
        "defaults": {
            "color": {"mode": "thresholds"},
            "mappings": [],
            "thresholds": {"mode": "absolute", "steps": steps},
            "unit": unit or "none",
        },
        "overrides": [],
    }
    p["options"] = {
        "colorMode": "background",
        "graphMode": "area",
        "justifyMode": "auto",
        "orientation": "auto",
        "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
        "textMode": "auto",
    }
    p["targets"] = targets
    if desc:
        p["description"] = desc
    return p


def bargauge(pid, title, x, y, w, h, targets, unit="percentunit", max_val=1, thresholds=None, desc=""):
    p = panel_base(pid, title, "bargauge", x, y, w, h)
    steps = thresholds or [
        {"color": "red", "value": None},
        {"color": "orange", "value": 0.5},
        {"color": "green", "value": 0.8},
    ]
    p["fieldConfig"] = {
        "defaults": {
            "color": {"mode": "thresholds"},
            "mappings": [],
            "max": max_val,
            "min": 0,
            "thresholds": {"mode": "absolute", "steps": steps},
            "unit": unit,
        },
        "overrides": [],
    }
    p["options"] = {
        "displayMode": "lcd",
        "maxVizHeight": 300,
        "minVizHeight": 16,
        "minVizWidth": 8,
        "namePlacement": "auto",
        "orientation": "horizontal",
        "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
        "showUnfilled": True,
        "sizing": "auto",
        "valueMode": "color",
    }
    p["targets"] = targets
    if desc:
        p["description"] = desc
    return p


def row(pid, title, y, collapsed=False):
    return {
        "id": pid,
        "title": title,
        "type": "row",
        "collapsed": collapsed,
        "gridPos": {"h": 1, "w": 24, "x": 0, "y": y},
        "panels": [],
    }


def text_panel(pid, title, x, y, w, h, content):
    p = panel_base(pid, title, "text", x, y, w, h)
    p["options"] = {"content": content, "mode": "markdown"}
    return p


def dim_var(name, label, metric_field, group_by, where, multi=True, include_all=True, regex=""):
    return {
        "current": {"selected": True, "text": "All", "value": "$__all"} if include_all else {},
        "datasource": DS,
        "definition": "- Blueking Monitor - 维度",
        "hide": 0,
        "includeAll": include_all,
        "label": label,
        "multi": multi,
        "name": name,
        "options": [],
        "query": {
            "metricConfig": {
                "data_source_label": "bk_monitor",
                "data_type_label": "time_series",
                "group_by": group_by,
                "metric_field": metric_field,
                "result_table_id": TABLE,
                "result_table_label": "application_check",
                "where": where,
            },
            "queryType": "dimension",
            "variables": "",
        },
        "refresh": 2,
        "regex": regex,
        "skipUrlSync": False,
        "sort": 1,
        "type": "query",
    }


def custom_var(name, label, options, multi=True, include_all=True):
    opts = [{"selected": False, "text": o, "value": o} for o in options]
    current = {"selected": True, "text": "All", "value": "$__all"} if include_all else {
        "selected": True, "text": options[0], "value": options[0]
    }
    return {
        "allValue": "",
        "current": current,
        "hide": 0,
        "includeAll": include_all,
        "label": label,
        "multi": multi,
        "name": name,
        "options": opts,
        "query": ",".join(options),
        "queryValue": "",
        "skipUrlSync": False,
        "type": "custom",
    }


RBAC_IPS = ["21.200.250.146", "21.200.250.152", "21.200.250.34"]


MSG_TH = [
    {"color": "green", "value": None},
    {"color": "orange", "value": 5000},
    {"color": "red", "value": 10000},
]
CPU_TH = [
    {"color": "green", "value": None},
    {"color": "orange", "value": 70},
    {"color": "red", "value": 90},
]
MEM_TH = [
    {"color": "green", "value": None},
    {"color": "orange", "value": 70},
    {"color": "red", "value": 85},
]
DISK_TH = [
    {"color": "green", "value": None},
    {"color": "orange", "value": 70},
    {"color": "red", "value": 85},
]
UTIL_TH = [
    {"color": "red", "value": None},
    {"color": "orange", "value": 0.5},
    {"color": "green", "value": 0.8},
]


def main():
    panels = []
    pid = 1

    panels.append(row(pid, "1. 队列积压", 0))
    pid += 1
    panels.append(
        timeseries(
            pid,
            "集群总积压消息",
            0,
            1,
            24,
            10,
            [
                target(
                    [
                        qc(
                            "nodeTotalMessagesValue",
                            ["bk_target_ip"],
                            "$tag_bk_target_ip",
                            method="MAX",
                            where=script_where(),
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
            threshold_line=True,
            desc="nodeTotalMessagesValue。这是集群总量，不是按队列拆开的积压。",
        )
    )
    pid += 1
    all_queue_where = script_where(
        [
            {"condition": "and", "key": "queueName", "method": "eq", "value": ["$queue"]},
            {"condition": "and", "key": "vhostName", "method": "eq", "value": ["$vhost"]},
        ]
    )
    panels.append(
        timeseries(
            pid,
            "所有队列积压（单图 Top30）",
            0,
            11,
            24,
            12,
            [
                target(
                    [
                        qc_top(
                            "queueMessagesValue",
                            ["vhostName", "queueName"],
                            "$tag_vhostName / $tag_queueName",
                            n=30,
                            method="MAX",
                            where=all_queue_where,
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
            threshold_line=True,
            desc="采集插件升级后出数：queueMessagesValue，按 vhost+queue 分组。903 条线会叠死图，这里只画积压 Top30；变量 queue/vhost 选 All。升级前会 No data。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "所有队列 Ready / Unacked Top30",
            0,
            23,
            24,
            10,
            [
                target(
                    [
                        qc_top(
                            "queueReadyValue",
                            ["vhostName", "queueName"],
                            "ready $tag_vhostName / $tag_queueName",
                            n=30,
                            method="MAX",
                            where=all_queue_where,
                        )
                    ],
                    ref="A",
                ),
                target(
                    [
                        qc_top(
                            "queueUnackedValue",
                            ["vhostName", "queueName"],
                            "unacked $tag_vhostName / $tag_queueName",
                            n=30,
                            method="MAX",
                            where=all_queue_where,
                        )
                    ],
                    ref="B",
                ),
            ],
            desc="Ready 才是真正没被消费的积压；Unacked 是在途。当前生产积压几乎全是 Unacked。升级采集前会 No data。",
        )
    )
    pid += 1
    panels.append(
        stat(
            pid,
            "集群消息积压 MAX",
            0,
            33,
            6,
            5,
            [
                target(
                    [
                        qc(
                            "nodeTotalMessagesValue",
                            [],
                            "cluster_max_messages",
                            method="MAX",
                            where=script_where(),
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
            desc="与告警策略一致：MAX(nodeTotalMessagesValue)。橙≥5000，红≥10000。",
        )
    )
    pid += 1
    panels.append(
        stat(
            pid,
            "MQ 进程内存 (MB)",
            6,
            33,
            6,
            5,
            [
                target(
                    [
                        qc(
                            "nodeMemoryUseMbValue",
                            ["bk_target_ip"],
                            "$tag_bk_target_ip",
                            method="MAX",
                            where=script_where(),
                        )
                    ]
                )
            ],
            unit="decmbytes",
            desc="脚本采集 nodeMemoryUseMbValue。146 节点通常明显高于另外两台。",
        )
    )
    pid += 1
    panels.append(
        stat(
            pid,
            "主机 CPU %",
            12,
            33,
            6,
            5,
            [
                target(
                    [
                        qc(
                            "usage",
                            ["bk_target_ip"],
                            "$tag_bk_target_ip",
                            method="MAX",
                            table="system.cpu_summary",
                            data_label="",
                            result_table_label="os",
                            where=host_where(),
                        )
                    ]
                )
            ],
            unit="percent",
            thresholds=CPU_TH,
        )
    )
    pid += 1
    panels.append(
        stat(
            pid,
            "主机内存 %",
            18,
            33,
            6,
            5,
            [
                target(
                    [
                        qc(
                            "pct_used",
                            ["bk_target_ip"],
                            "$tag_bk_target_ip",
                            method="MAX",
                            table="system.mem",
                            data_label="",
                            result_table_label="os",
                            where=host_where(),
                        )
                    ]
                )
            ],
            unit="percent",
            thresholds=MEM_TH,
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "节点 MQ 内存使用",
            0,
            38,
            24,
            8,
            [
                target(
                    [
                        qc(
                            "nodeMemoryUseMbValue",
                            ["bk_target_ip"],
                            "$tag_bk_target_ip MB",
                            method="MAX",
                            where=script_where(),
                        )
                    ],
                    ref="A",
                ),
                target(
                    [
                        qc(
                            "nodeMemoryUseGbValue",
                            ["bk_target_ip"],
                            "$tag_bk_target_ip GB",
                            method="MAX",
                            where=script_where(),
                        )
                    ],
                    ref="B",
                ),
            ],
            unit="decmbytes",
            desc="nodeMemoryUseMbValue / nodeMemoryUseGbValue，用于判断内存告警水位和节点不均衡。",
        )
    )
    pid += 1

    panels.append(row(pid, "2. 主机健康", 46))
    pid += 1
    host_panels = [
        ("CPU 使用率", "usage", "system.cpu_summary", "percent", CPU_TH, None),
        ("应用程序内存使用率", "pct_used", "system.mem", "percent", MEM_TH, None),
        ("磁盘 / 使用率", "in_use", "system.disk", "percent", DISK_TH, "mount_point=/"),
        ("磁盘 /data 使用率", "in_use", "system.disk", "percent", DISK_TH, "mount_point=/data"),
        ("I/O 使用率", "util", "system.io", "percent", CPU_TH, None),
        ("TCP ESTABLISHED", "cur_tcp_estab", "system.netstat", "none", None, None),
        ("TCP TIME_WAIT", "cur_tcp_timewait", "system.netstat", "none", None, None),
        ("网卡入流量", "speed_recv", "system.net", "Bps", None, None),
        ("网卡出流量", "speed_sent", "system.net", "Bps", None, None),
        ("应用程序内存使用量", "used", "system.mem", "bytes", None, None),
    ]
    y = 47
    for i, (title, field, table, unit, th, extra_dim) in enumerate(host_panels):
        x = (i % 4) * 6
        if i and i % 4 == 0:
            y += 7
        where = host_where()
        group = ["bk_target_ip"]
        if extra_dim == "mount_point=/":
            where = host_where() + [{"condition": "and", "key": "mount_point", "method": "eq", "value": ["/"]}]
        elif extra_dim == "mount_point=/data":
            where = host_where() + [{"condition": "and", "key": "mount_point", "method": "eq", "value": ["/data"]}]
        panels.append(
            timeseries(
                pid,
                title,
                x,
                y,
                6,
                7,
                [
                    target(
                        [
                            qc(
                                field,
                                group,
                                "$tag_bk_target_ip",
                                method="MAX",
                                table=table,
                                data_label="",
                                result_table_label="os",
                                where=where,
                            )
                        ]
                    )
                ],
                unit=unit,
                thresholds=th,
            )
        )
        pid += 1
    y += 7

    panels.append(row(pid, "3. 已采集队列的 consumer utilisation", y))
    pid += 1
    y += 1
    all_targets = []
    for i, (field, _vhost) in enumerate(QUEUE_METRICS):
        all_targets.append(
            target(
                [
                    qc(
                        field,
                        ["vhostName", "queueName"],
                        "$tag_vhostName / $tag_queueName",
                        method="MAX",
                        where=script_where(
                            [
                                {"condition": "and", "key": "queueName", "method": "eq", "value": ["$queue"]},
                                {"condition": "and", "key": "vhostName", "method": "eq", "value": ["$vhost"]},
                            ]
                        ),
                    )
                ],
                ref=chr(ord("A") + i),
            )
        )
    panels.append(
        bargauge(
            pid,
            "全部队列当前 consumer utilisation",
            0,
            y,
            24,
            8,
            all_targets,
            desc="按 vhost + queue 展示。变量 queue/vhost 选 All 即可看到采集到的全部队列。红<0.5，橙<0.8，绿≥0.8。",
        )
    )
    pid += 1
    y += 8
    panels.append(
        timeseries(
            pid,
            "全部队列 consumer utilisation 历史",
            0,
            y,
            24,
            10,
            all_targets,
            unit="percentunit",
            max_val=1,
            thresholds=UTIL_TH,
            desc="同一套队列指标的时序。用于对照总积压尖峰时，是哪条队列消费者跟不上。",
        )
    )
    pid += 1
    y += 10

    for title, field, vhost in [
        ("rbac-core 队列", "queueRbacCoreConsumerUtilisationValue", "rbac-core"),
        ("rbac-gray-core 队列", "queueRbacGrayCoreConsumerUtilisationValue", "rbac-gray-core"),
    ]:
        panels.append(row(pid, f"4. {title}", y))
        pid += 1
        y += 1
        t = [
            target(
                [
                    qc(
                        field,
                        ["queueName"],
                        "$tag_queueName",
                        method="MAX",
                        where=script_where(
                            [
                                {"condition": "and", "key": "vhostName", "method": "eq", "value": [vhost]},
                                {"condition": "and", "key": "queueName", "method": "eq", "value": ["$queue"]},
                            ]
                        ),
                    )
                ]
            )
        ]
        panels.append(
            bargauge(
                pid,
                f"{vhost} 当前利用率",
                0,
                y,
                24,
                6,
                t,
                desc=f"{field}，vhost={vhost}。当前生产有点的队列：engine-taskBuildConsumer、startDevCloudConsumer。",
            )
        )
        pid += 1
        y += 6
        panels.append(
            timeseries(
                pid,
                f"{vhost} 利用率历史",
                0,
                y,
                24,
                8,
                [
                    target(
                        [
                            qc(
                                field,
                                ["queueName"],
                                "$tag_queueName",
                                method="AVG",
                                where=script_where(
                                    [
                                        {"condition": "and", "key": "vhostName", "method": "eq", "value": [vhost]},
                                        {"condition": "and", "key": "queueName", "method": "eq", "value": ["$queue"]},
                                    ]
                                ),
                            )
                        ]
                    )
                ],
                unit="percentunit",
                max_val=1,
                thresholds=UTIL_TH,
            )
        )
        pid += 1
        y += 8

    panels.append(row(pid, "5. 其它 vhost（采集已定义，当前可能无点）", y, collapsed=True))
    pid += 1
    y += 1
    other = [m for m in QUEUE_METRICS if m[1] not in ("rbac-core", "rbac-gray-core")]
    for i, (field, vhost) in enumerate(other):
        x = (i % 2) * 12
        if i and i % 2 == 0:
            y += 8
        panels.append(
            timeseries(
                pid,
                f"{vhost} / {field}",
                x,
                y,
                12,
                8,
                [
                    target(
                        [
                            qc(
                                field,
                                ["queueName", "vhostName"],
                                "$tag_vhostName / $tag_queueName",
                                method="MAX",
                                where=script_where(),
                            )
                        ]
                    )
                ],
                unit="percentunit",
                max_val=1,
                thresholds=UTIL_TH,
                desc="该指标在当前生产三节点近 24h 无维度值；采集脚本覆盖后会自动出数。",
            )
        )
        pid += 1

    dashboard = {
        "annotations": {
            "list": [
                {
                    "builtIn": 1,
                    "datasource": {"type": "grafana", "uid": "-- Grafana --"},
                    "enable": True,
                    "hide": True,
                    "iconColor": "rgba(0, 211, 255, 1)",
                    "name": "Annotations & Alerts",
                    "target": {"limit": 100, "matchAny": False, "tags": [], "type": "dashboard"},
                    "type": "dashboard",
                }
            ]
        },
        "description": "蓝盾 MQ 生产集群（pipeline正式环境-rbac）监控。采集 1131 / 节点 21.200.250.146,152,34。所有队列单图依赖插件新增 queueMessagesValue，升级采集前该图 No data。",
        "editable": True,
        "fiscalYearStartMonth": 0,
        "graphTooltip": 1,
        "id": 1612,
        "links": [],
        "liveNow": False,
        "panels": panels,
        "refresh": "1m",
        "schemaVersion": 37,
        "style": "dark",
        "tags": ["bk-ci", "rabbitmq", "production"],
        "templating": {
            "list": [
                dim_var(
                    "cluster_ip",
                    "节点IP",
                    "nodeTotalMessagesValue",
                    "bk_target_ip",
                    [
                        {"key": "bk_collect_config_id", "method": "eq", "value": [COLLECT]},
                        {
                            "condition": "and",
                            "key": "bk_target_ip",
                            "method": "eq",
                            "value": RBAC_IPS,
                        },
                    ],
                ),
                custom_var(
                    "vhost",
                    "vhost",
                    [v for _, v in QUEUE_METRICS],
                ),
                dim_var(
                    "queue",
                    "队列",
                    "queueRbacCoreConsumerUtilisationValue",
                    "queueName",
                    [
                        {"key": "bk_collect_config_id", "method": "eq", "value": [COLLECT]},
                        {"condition": "and", "key": "bk_target_ip", "method": "eq", "value": ["$cluster_ip"]},
                    ],
                ),
            ]
        },
        "time": {"from": "now-6h", "to": "now"},
        "timepicker": {},
        "timezone": "Asia/Shanghai",
        "title": "蓝盾MQ生产集群监控",
        "uid": "mOnpn8q4z",
        "version": 14,
        "weekStart": "",
    }

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(dashboard, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"wrote {OUT} panels={len(panels)}")


if __name__ == "__main__":
    main()
