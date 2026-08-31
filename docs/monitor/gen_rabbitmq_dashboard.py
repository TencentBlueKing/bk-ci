#!/usr/bin/env python3
"""蓝盾 MQ 看板（bkmonitor.woa.com / 100205 / 采集 4894）

第一组：只切 vhost/queue，范围钉死两个生产集群。
第二组：mq-node、bkdevops_mq_mr 各一组，overview 按集群 MAX，节点图才按 nodeName。
"""

import json

DS = {"type": "bkmonitor-timeseries-datasource", "uid": "87moJ-rGk"}
LABEL = "script_devops_rabbitmq_plugin"
TABLE = f"{LABEL}.__default__"
COLLECT = "4894"
OUT = "/Users/royalhuang/devops/dev/bk-ci-ext/docs/monitor/grafana-bkci-rabbitmq.json"

# 采集脚本升级前 cluster 仍是单节点名，过滤用 nodeName 子串
CLUSTERS = [
    ("mq-node", "mq-node"),
    ("bkdevops_mq_mr", "bkdevops_mq_mr"),
]
KEEP_CLUSTERS_REG = "mq-node|bkdevops_mq_mr"

MSG_TH = [
    {"color": "green", "value": None},
    {"color": "orange", "value": 5000},
    {"color": "red", "value": 10000},
]


def where_base(extra=None):
    w = [{"condition": "and", "key": "bk_collect_config_id", "method": "eq", "value": [COLLECT]}]
    if extra:
        w.extend(extra)
    return w


def keep_w():
    return [{"condition": "and", "key": "nodeName", "method": "reg", "value": [KEEP_CLUSTERS_REG]}]


def cluster_w(key):
    return [{"condition": "and", "key": "nodeName", "method": "reg", "value": [key]}]


def qc(metric, group_by, alias, method="MAX", extra_where=None, top=None):
    cfg = {
        "alias": alias,
        "data_source_label": "bk_monitor",
        "data_type_label": "time_series",
        "data_label": LABEL,
        "display": True,
        "filter_dict": {},
        "functions": [{"id": "time_shift", "params": [{"id": "n"}]}],
        "group_by": group_by,
        "interval": "auto",
        "interval_unit": "s",
        "method": method,
        "metric_field": metric,
        "refId": "a",
        "result_table_id": TABLE,
        "result_table_label": "application_check",
        "time_field": "time",
        "where": where_base(extra_where),
    }
    if top:
        cfg["functions"] = [
            {"id": "time_shift", "params": [{"id": "n"}]},
            {"id": "top", "params": [{"id": "n", "value": str(top)}]},
        ]
    return cfg


def target(query_configs, ref="A"):
    return {"datasource": DS, "mode": "ui", "query_configs": query_configs, "refId": ref, "source": ""}


def timeseries(pid, title, x, y, w, h, targets, unit="none", thresholds=None, desc=""):
    return {
        "id": pid,
        "title": title,
        "type": "timeseries",
        "datasource": DS,
        "description": desc,
        "gridPos": {"h": h, "w": w, "x": x, "y": y},
        "pluginVersion": "10.0.0",
        "fieldConfig": {
            "defaults": {
                "color": {"mode": "palette-classic"},
                "custom": {
                    "drawStyle": "line",
                    "fillOpacity": 10,
                    "lineWidth": 2,
                    "showPoints": "auto",
                    "spanNulls": True,
                    "stacking": {"group": "A", "mode": "none"},
                    "thresholdsStyle": {"mode": "line+area" if thresholds else "off"},
                },
                "thresholds": {"mode": "absolute", "steps": thresholds or [{"color": "green", "value": None}]},
                "unit": unit,
            },
            "overrides": [],
        },
        "options": {
            "legend": {
                "calcs": ["lastNotNull", "max"],
                "displayMode": "table",
                "placement": "bottom",
                "showLegend": True,
            },
            "tooltip": {"mode": "multi", "sort": "desc"},
        },
        "targets": targets,
    }


def stat(pid, title, x, y, w, h, targets, unit="none", thresholds=None):
    return {
        "id": pid,
        "title": title,
        "type": "stat",
        "datasource": DS,
        "gridPos": {"h": h, "w": w, "x": x, "y": y},
        "pluginVersion": "10.0.0",
        "fieldConfig": {
            "defaults": {
                "color": {"mode": "thresholds"},
                "thresholds": {"mode": "absolute", "steps": thresholds or [{"color": "green", "value": None}]},
                "unit": unit,
            },
            "overrides": [],
        },
        "options": {
            "colorMode": "background",
            "graphMode": "area",
            "orientation": "auto",
            "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
            "textMode": "auto",
        },
        "targets": targets,
    }


def row(pid, title, y, collapsed=False):
    return {
        "id": pid,
        "title": title,
        "type": "row",
        "collapsed": collapsed,
        "gridPos": {"h": 1, "w": 24, "x": 0, "y": y},
        "panels": [],
    }


def dim_var(name, label, metric, group_by, extra_where=None, multi=True):
    where = where_base(extra_where)
    if where:
        where = [{k: v for k, v in where[0].items() if k != "condition"}] + where[1:]
    return {
        "allValue": ".*",
        "current": {"selected": True, "text": "All", "value": "$__all"},
        "datasource": DS,
        "definition": "- Blueking Monitor - 维度",
        "hide": 0,
        "includeAll": True,
        "label": label,
        "multi": multi,
        "name": name,
        "options": [],
        "query": {
            "metricConfig": {
                "data_source_label": "bk_monitor",
                "data_type_label": "time_series",
                "group_by": group_by,
                "metric_field": metric,
                "result_table_id": TABLE,
                "result_table_label": "application_check",
                "where": where,
            },
            "queryType": "dimension",
            "variables": "",
        },
        "refresh": 2,
        "regex": "",
        "skipUrlSync": False,
        "sort": 1,
        "type": "query",
    }


def add_cluster_section(panels, pid, y, title, key):
    w = keep_w() + cluster_w(key)
    panels.append(row(pid, title, y))
    pid += 1
    y += 1

    stats = [
        ("集群消息总数", "rabbitmq_overview_messages", None),
        ("Ready", "rabbitmq_overview_messages_ready", None),
        ("Unacked", "rabbitmq_overview_messages_unacked", None),
        ("连接", "rabbitmq_overview_connections", "rabbitmq_overview_consumers"),
    ]
    for i, (stitle, field, extra) in enumerate(stats):
        if extra:
            tgts = [
                target([qc(field, [], f"conn {key}", extra_where=w)], "A"),
                target([qc(extra, [], f"csm {key}", extra_where=w)], "B"),
            ]
        else:
            tgts = [target([qc(field, [], key, extra_where=w)])]
        panels.append(
            stat(pid, stitle, i * 6, y, 6, 5, tgts, thresholds=MSG_TH if i < 3 else None)
        )
        pid += 1
    y += 5

    panels.append(
        timeseries(
            pid,
            f"{key} 积压与吞吐",
            0,
            y,
            12,
            8,
            [
                target([qc("rabbitmq_overview_messages", [], f"msg {key}", extra_where=w)], "A"),
                target(
                    [qc("rabbitmq_overview_messages_publish_rate", [], f"pub {key}", extra_where=w)],
                    "B",
                ),
                target(
                    [qc("rabbitmq_overview_messages_deliver_rate", [], f"del {key}", extra_where=w)],
                    "C",
                ),
            ],
            thresholds=MSG_TH,
            desc="按集群聚合：三台节点 overview 相同，用 MAX，不要 SUM。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            f"{key} 节点内存",
            12,
            y,
            12,
            8,
            [target([qc("rabbitmq_node_mem_used", ["nodeName"], "$tag_nodeName", extra_where=w)])],
            unit="bytes",
            desc="节点级指标才按 nodeName 拆。",
        )
    )
    pid += 1
    y += 8

    panels.append(
        timeseries(
            pid,
            f"{key} 内存/磁盘告警",
            0,
            y,
            12,
            7,
            [
                target([qc("rabbitmq_node_mem_alarm", ["nodeName"], "mem $tag_nodeName", extra_where=w)], "A"),
                target(
                    [qc("rabbitmq_node_disk_free_alarm", ["nodeName"], "disk $tag_nodeName", extra_where=w)],
                    "B",
                ),
            ],
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            f"{key} 连接 / 通道 / 队列数",
            12,
            y,
            12,
            7,
            [
                target([qc("rabbitmq_overview_connections", [], "conn", extra_where=w)], "A"),
                target([qc("rabbitmq_overview_channels", [], "chan", extra_where=w)], "B"),
                target([qc("rabbitmq_overview_queues", [], "queues", extra_where=w)], "C"),
            ],
        )
    )
    pid += 1
    y += 7
    return pid, y


def main():
    panels = []
    pid = 1
    y = 0

    scope = keep_w()
    vhost_w = scope + [{"condition": "and", "key": "vhostName", "method": "reg", "value": ["$vhost"]}]
    queue_w = vhost_w + [{"condition": "and", "key": "queueName", "method": "reg", "value": ["$queue"]}]

    panels.append(row(pid, "1. 按 vhost 看队列（仅 mq-node / bkdevops_mq_mr）", y))
    pid += 1
    y += 1

    for i, (title, field) in enumerate(
        [
            ("vhost 消息总数", "rabbitmq_vhost_messages"),
            ("vhost Ready", "rabbitmq_vhost_messages_ready"),
            ("vhost Unacked", "rabbitmq_vhost_messages_unacked"),
            ("队列积压（当前 vhost）", "rabbitmq_queue_messages"),
        ]
    ):
        panels.append(
            stat(
                pid,
                title,
                i * 6,
                y,
                6,
                5,
                [target([qc(field, ["vhostName"], "$tag_vhostName", extra_where=vhost_w)])],
                thresholds=MSG_TH if i < 3 else None,
            )
        )
        pid += 1
    y += 5

    panels.append(
        timeseries(
            pid,
            "各 vhost 积压",
            0,
            y,
            24,
            8,
            [target([qc("rabbitmq_vhost_messages", ["vhostName"], "$tag_vhostName", extra_where=vhost_w)])],
            thresholds=MSG_TH,
            desc="已限定 mq-node / bkdevops_mq_mr。切 vhost 即切环境。",
        )
    )
    pid += 1
    y += 8

    panels.append(
        timeseries(
            pid,
            "当前 vhost 下所有队列积压 Top30",
            0,
            y,
            24,
            12,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages",
                            ["vhostName", "queueName"],
                            "$tag_vhostName / $tag_queueName",
                            extra_where=queue_w,
                            top=30,
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
        )
    )
    pid += 1
    y += 12

    panels.append(
        timeseries(
            pid,
            "当前 vhost 队列 Ready / Unacked Top30",
            0,
            y,
            24,
            10,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_ready",
                            ["queueName"],
                            "ready $tag_queueName",
                            extra_where=queue_w,
                            top=30,
                        )
                    ],
                    "A",
                ),
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_unacked",
                            ["queueName"],
                            "unacked $tag_queueName",
                            extra_where=queue_w,
                            top=30,
                        )
                    ],
                    "B",
                ),
            ],
            desc="Ready=未被消费；Unacked=在途。",
        )
    )
    pid += 1
    y += 10

    panels.append(
        timeseries(
            pid,
            "当前 vhost 消费者利用率",
            0,
            y,
            24,
            8,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_consumer_utilisation",
                            ["queueName"],
                            "$tag_queueName",
                            extra_where=queue_w,
                            top=30,
                        )
                    ]
                )
            ],
            unit="percentunit",
        )
    )
    pid += 1
    y += 8

    for name, key in CLUSTERS:
        pid, y = add_cluster_section(panels, pid, y, f"2. 集群 {name}", key)

    dashboard = {
        "annotations": {"list": []},
        "description": "采集 4894。只看 mq-node / bkdevops_mq_mr。集群图按集群 MAX，不按节点拆 overview。",
        "editable": True,
        "fiscalYearStartMonth": 0,
        "graphTooltip": 1,
        "id": 100122,
        "links": [],
        "liveNow": False,
        "panels": panels,
        "refresh": "1m",
        "schemaVersion": 37,
        "style": "dark",
        "tags": ["bk-ci", "rabbitmq"],
        "templating": {
            "list": [
                dim_var(
                    "vhost",
                    "vhost / 环境",
                    "rabbitmq_vhost_messages",
                    "vhostName",
                    extra_where=keep_w(),
                ),
                dim_var(
                    "queue",
                    "队列",
                    "rabbitmq_queue_messages",
                    "queueName",
                    extra_where=keep_w()
                    + [{"condition": "and", "key": "vhostName", "method": "reg", "value": ["$vhost"]}],
                ),
            ]
        },
        "time": {"from": "now-1h", "to": "now"},
        "timepicker": {},
        "timezone": "Asia/Shanghai",
        "title": "MQ监控",
        "uid": "bfwllw3guyosgf",
        "version": 5,
        "weekStart": "",
    }

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(dashboard, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"wrote {OUT} panels={len(panels)}")


if __name__ == "__main__":
    main()
