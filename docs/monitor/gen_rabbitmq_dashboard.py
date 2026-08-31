#!/usr/bin/env python3
"""蓝盾 MQ 看板（bkmonitor.woa.com / 100205 / 采集 4894）

布局对齐日志看板：半宽时序为主，不要一上来铺一排绿色格子。
四个业务集群跟 Management URL 对齐，按 nodeName 精确前缀过滤（忽略节点名里的 rbac/ext 子串）。
"""

import json

DS = {"type": "bkmonitor-timeseries-datasource", "uid": "87moJ-rGk"}
LABEL = "script_devops_rabbitmq_plugin"
TABLE = f"{LABEL}.__default__"
COLLECT = "4894"
OUT = "/Users/royalhuang/devops/dev/bk-ci-ext/docs/monitor/grafana-bkci-rabbitmq.json"

# title = 监控展示名；node_reg 必须能区分 bkdevops_rbac_mq_ 与 bkdevops_ext_rbac_mq_
CLUSTERS = [
    {
        "title": "auto&stream",
        "node_reg": r"rabbit@bkdevops_core_mq_",
        "ips": ["30.186.110.10", "30.186.110.65", "30.189.37.13"],
    },
    {
        "title": "rbac",
        "node_reg": r"rabbit@bkdevops_rbac_mq_",
        "ips": ["21.200.250.146", "21.200.250.152", "21.200.250.34"],
    },
    {
        "title": "vip",
        "node_reg": r"rabbit@mq-node",
        "ips": ["30.186.111.174", "30.186.110.252", "30.186.110.74"],
    },
    {
        "title": "ext",
        "node_reg": r"rabbit@bkdevops_mq_mr_",
        "ips": ["9.136.132.205", "9.136.162.33", "9.136.161.63"],
    },
]
KEEP_NODES_REG = "|".join(c["node_reg"] for c in CLUSTERS)

# 与告警一致：1万预警 / 5万致命
MSG_TH = [
    {"color": "green", "value": None},
    {"color": "orange", "value": 10000},
    {"color": "red", "value": 50000},
]
ALARM_TH = [
    {"color": "green", "value": None},
    {"color": "red", "value": 1},
]


def where_base(extra=None):
    w = [{"condition": "and", "key": "bk_collect_config_id", "method": "eq", "value": [COLLECT]}]
    if extra:
        w.extend(extra)
    return w


def node_w(node_reg):
    return [{"condition": "and", "key": "nodeName", "method": "reg", "value": [node_reg]}]


def all_w():
    return node_w(KEEP_NODES_REG)


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
                    "axisBorderShow": False,
                    "axisCenteredZero": False,
                    "axisColorMode": "text",
                    "drawStyle": "line",
                    "fillOpacity": 10,
                    "gradientMode": "none",
                    "lineInterpolation": "linear",
                    "lineWidth": 1,
                    "pointSize": 5,
                    "showPoints": "never",
                    "spanNulls": True,
                    "stacking": {"group": "A", "mode": "none"},
                    "thresholdsStyle": {"mode": "line+area" if thresholds else "off"},
                },
                "min": 0,
                "thresholds": {"mode": "absolute", "steps": thresholds or [{"color": "green", "value": None}]},
                "unit": unit,
            },
            "overrides": [],
        },
        "options": {
            "legend": {
                "calcs": ["lastNotNull", "mean", "max"],
                "displayMode": "table",
                "placement": "bottom",
                "showLegend": True,
            },
            "tooltip": {"mode": "multi", "sort": "desc"},
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


def cluster_targets(metric, alias_suffix=""):
    refs = "ABCDEFGH"
    tgts = []
    for i, c in enumerate(CLUSTERS):
        alias = f"{c['title']}{alias_suffix}"
        tgts.append(target([qc(metric, [], alias, extra_where=node_w(c["node_reg"]))], refs[i]))
    return tgts


def add_cluster_section(panels, pid, y, cluster, row_title, collapsed=False):
    title = cluster["title"]
    w = node_w(cluster["node_reg"])
    panels.append(row(pid, row_title, y, collapsed=collapsed))
    pid += 1
    y += 1

    panels.append(
        timeseries(
            pid,
            f"{title} 积压与吞吐",
            0,
            y,
            12,
            9,
            [
                target([qc("rabbitmq_overview_messages", [], "messages", extra_where=w)], "A"),
                target([qc("rabbitmq_overview_messages_ready", [], "ready", extra_where=w)], "B"),
                target([qc("rabbitmq_overview_messages_unacked", [], "unacked", extra_where=w)], "C"),
                target([qc("rabbitmq_overview_messages_publish_rate", [], "publish/s", extra_where=w)], "D"),
                target([qc("rabbitmq_overview_messages_deliver_rate", [], "deliver/s", extra_where=w)], "E"),
                target([qc("rabbitmq_overview_messages_ack_rate", [], "ack/s", extra_where=w)], "F"),
            ],
            thresholds=MSG_TH,
            desc="overview 三节点相同，MAX 聚合。橙≥1万预警，红≥5万致命。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            f"{title} 队列积压 Top20",
            12,
            y,
            12,
            9,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages",
                            ["vhostName", "queueName"],
                            "$tag_vhostName / $tag_queueName",
                            extra_where=w,
                            top=20,
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
            desc="按队列 MAX，避免三节点 SUM。",
        )
    )
    pid += 1
    y += 9

    panels.append(
        timeseries(
            pid,
            f"{title} 节点内存",
            0,
            y,
            12,
            8,
            [
                target([qc("rabbitmq_node_mem_used", ["nodeName"], "used $tag_nodeName", extra_where=w)], "A"),
                target([qc("rabbitmq_node_mem_limit", ["nodeName"], "limit $tag_nodeName", extra_where=w)], "B"),
            ],
            unit="bytes",
            desc="节点级才按 nodeName 拆。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            f"{title} Ready / Unacked Top20",
            12,
            y,
            12,
            8,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_ready",
                            ["queueName"],
                            "ready $tag_queueName",
                            extra_where=w,
                            top=20,
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
                            extra_where=w,
                            top=20,
                        )
                    ],
                    "B",
                ),
            ],
            desc="Ready=待消费；Unacked=在途。",
        )
    )
    pid += 1
    y += 8

    panels.append(
        timeseries(
            pid,
            f"{title} 连接 / 通道 / 队列 / 消费者",
            0,
            y,
            12,
            8,
            [
                target([qc("rabbitmq_overview_connections", [], "conn", extra_where=w)], "A"),
                target([qc("rabbitmq_overview_channels", [], "chan", extra_where=w)], "B"),
                target([qc("rabbitmq_overview_queues", [], "queues", extra_where=w)], "C"),
                target([qc("rabbitmq_overview_consumers", [], "consumers", extra_where=w)], "D"),
            ],
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            f"{title} 节点告警 / FD / 分区",
            12,
            y,
            12,
            8,
            [
                target([qc("rabbitmq_node_mem_alarm", ["nodeName"], "memAlarm $tag_nodeName", extra_where=w)], "A"),
                target(
                    [qc("rabbitmq_node_disk_free_alarm", ["nodeName"], "diskAlarm $tag_nodeName", extra_where=w)],
                    "B",
                ),
                target([qc("rabbitmq_node_partitions", ["nodeName"], "partitions $tag_nodeName", extra_where=w)], "C"),
                target([qc("rabbitmq_node_fd_used", ["nodeName"], "fd $tag_nodeName", extra_where=w)], "D"),
            ],
            thresholds=ALARM_TH,
            desc="mem/disk alarm=1 即水位告警；partitions>0 脑裂。",
        )
    )
    pid += 1
    y += 8
    return pid, y


def main():
    panels = []
    pid = 1
    y = 0
    scope = all_w()
    vhost_w = scope + [{"condition": "and", "key": "vhostName", "method": "reg", "value": ["$vhost"]}]
    queue_w = vhost_w + [{"condition": "and", "key": "queueName", "method": "reg", "value": ["$queue"]}]

    panels.append(row(pid, "① 积压总览 / Backlog（四集群对照）", y))
    pid += 1
    y += 1

    panels.append(
        timeseries(
            pid,
            "跨集群队列积压 Top20",
            0,
            y,
            12,
            9,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages",
                            ["vhostName", "queueName"],
                            "$tag_vhostName / $tag_queueName",
                            extra_where=scope,
                            top=20,
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
            desc="四集群合计 Top20。下钻看第②组或切 vhost。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "跨集群消费者利用率 Top20",
            12,
            y,
            12,
            9,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_consumer_utilisation",
                            ["vhostName", "queueName"],
                            "$tag_vhostName / $tag_queueName",
                            extra_where=scope,
                            top=20,
                        )
                    ]
                )
            ],
            unit="percentunit",
        )
    )
    pid += 1
    y += 9

    panels.append(
        timeseries(
            pid,
            "各集群消息总数",
            0,
            y,
            12,
            9,
            cluster_targets("rabbitmq_overview_messages"),
            thresholds=MSG_TH,
            desc="每集群 MAX(overview)。橙≥1万，红≥5万。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "各集群 Ready / Unacked",
            12,
            y,
            12,
            9,
            cluster_targets("rabbitmq_overview_messages_ready", " ready")
            + [
                target(
                    [qc("rabbitmq_overview_messages_unacked", [], f"{c['title']} unacked", extra_where=node_w(c["node_reg"]))],
                    ref,
                )
                for c, ref in zip(CLUSTERS, "EFGH")
            ],
            thresholds=MSG_TH,
        )
    )
    pid += 1
    y += 9

    panels.append(
        timeseries(
            pid,
            "各集群发布 / 投递 / Ack 速率",
            0,
            y,
            12,
            8,
            cluster_targets("rabbitmq_overview_messages_publish_rate", " pub")
            + [
                target(
                    [
                        qc(
                            "rabbitmq_overview_messages_deliver_rate",
                            [],
                            f"{c['title']} del",
                            extra_where=node_w(c["node_reg"]),
                        )
                    ],
                    ref,
                )
                for c, ref in zip(CLUSTERS, "EFGH")
            ],
            unit="ops",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "各集群连接 / 消费者",
            12,
            y,
            12,
            8,
            cluster_targets("rabbitmq_overview_connections", " conn")
            + [
                target(
                    [qc("rabbitmq_overview_consumers", [], f"{c['title']} csm", extra_where=node_w(c["node_reg"]))],
                    ref,
                )
                for c, ref in zip(CLUSTERS, "EFGH")
            ],
        )
    )
    pid += 1
    y += 8

    panels.append(row(pid, "② 按 vhost / 队列下钻", y))
    pid += 1
    y += 1

    panels.append(
        timeseries(
            pid,
            "各 vhost 积压",
            0,
            y,
            12,
            9,
            [target([qc("rabbitmq_vhost_messages", ["vhostName"], "$tag_vhostName", extra_where=vhost_w)])],
            thresholds=MSG_TH,
            desc="四集群合计。切顶部 vhost 即切环境。",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "当前 vhost 队列积压 Top20",
            12,
            y,
            12,
            9,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages",
                            ["vhostName", "queueName"],
                            "$tag_vhostName / $tag_queueName",
                            extra_where=queue_w,
                            top=20,
                        )
                    ]
                )
            ],
            thresholds=MSG_TH,
        )
    )
    pid += 1
    y += 9

    panels.append(
        timeseries(
            pid,
            "当前 vhost Ready / Unacked Top20",
            0,
            y,
            12,
            8,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_ready",
                            ["queueName"],
                            "ready $tag_queueName",
                            extra_where=queue_w,
                            top=20,
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
                            top=20,
                        )
                    ],
                    "B",
                ),
            ],
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "当前 vhost 消费者利用率 / 消费者数",
            12,
            y,
            12,
            8,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_consumer_utilisation",
                            ["queueName"],
                            "util $tag_queueName",
                            extra_where=queue_w,
                            top=20,
                        )
                    ],
                    "A",
                ),
                target(
                    [
                        qc(
                            "rabbitmq_queue_consumers",
                            ["queueName"],
                            "csm $tag_queueName",
                            extra_where=queue_w,
                            top=20,
                        )
                    ],
                    "B",
                ),
            ],
            desc="利用率偏低且积压上涨 = 消费跟不上。",
        )
    )
    pid += 1
    y += 8

    panels.append(
        timeseries(
            pid,
            "当前 vhost 队列 publish / deliver 速率",
            0,
            y,
            12,
            8,
            [
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_publish_rate",
                            ["queueName"],
                            "pub $tag_queueName",
                            extra_where=queue_w,
                            top=15,
                        )
                    ],
                    "A",
                ),
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_deliver_rate",
                            ["queueName"],
                            "del $tag_queueName",
                            extra_where=queue_w,
                            top=15,
                        )
                    ],
                    "B",
                ),
            ],
            unit="ops",
        )
    )
    pid += 1
    panels.append(
        timeseries(
            pid,
            "不可路由 / 重投递",
            12,
            y,
            12,
            8,
            [
                target([qc("rabbitmq_overview_return_unroutable_rate", [], "unroutable/s", extra_where=scope)], "A"),
                target(
                    [
                        qc(
                            "rabbitmq_queue_messages_redeliver_rate",
                            ["queueName"],
                            "redeliver $tag_queueName",
                            extra_where=queue_w,
                            top=15,
                        )
                    ],
                    "B",
                ),
            ],
            unit="ops",
            desc="unroutable 升高说明路由/绑定异常；redeliver 升高说明消费失败重试。",
        )
    )
    pid += 1
    y += 8

    cluster_rows = [
        "③ auto&stream",
        "④ rbac",
        "⑤ vip",
        "⑥ ext",
    ]
    for i, cluster in enumerate(CLUSTERS):
        pid, y = add_cluster_section(panels, pid, y, cluster, cluster_rows[i], collapsed=False)

    dashboard = {
        "annotations": {"list": []},
        "description": (
            "采集 4894。集群按 IP 划分："
            "auto&stream / rbac / vip / ext（对齐 /mq/{auto,rbac,vip,ext}/）。"
            "overview 用 MAX。告警阈值 1万/5万。"
        ),
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
                dim_var("vhost", "vhost / 环境", "rabbitmq_vhost_messages", "vhostName", extra_where=all_w()),
                dim_var(
                    "queue",
                    "队列",
                    "rabbitmq_queue_messages",
                    "queueName",
                    extra_where=all_w()
                    + [{"condition": "and", "key": "vhostName", "method": "reg", "value": ["$vhost"]}],
                ),
            ]
        },
        "time": {"from": "now-1h", "to": "now"},
        "timepicker": {},
        "timezone": "Asia/Shanghai",
        "title": "MQ监控",
        "uid": "bfwllw3guyosgf",
        "version": 8,
        "weekStart": "",
    }

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(dashboard, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"wrote {OUT} panels={len(panels)}")


if __name__ == "__main__":
    main()
