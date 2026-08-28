#!/usr/bin/env python3
"""Generate BKMonitor plugin metric/dimension import JSON for devops_rabbitmq_plugin."""

import json

OUT = "/Users/royalhuang/devops/dev/bk-ci-ext/docs/monitor/devops_rabbitmq_plugin_metrics.json"

DIM_DESC = {
    "clusterName": "RabbitMQ 集群名（overview.cluster_name）",
    "nodeName": "上报节点 / Erlang 节点名",
    "vhostName": "vhost",
    "queueName": "队列名",
    "queueNode": "队列所在节点",
    "queueType": "队列类型 classic/quorum",
    "reason": "采集失败原因",
}


def dim_field(name):
    return {
        "description": DIM_DESC[name],
        "monitor_type": "dimension",
        "name": name,
        "type": "string",
        "unit": "none",
        "is_active": True,
    }


def metric(name, desc, unit, dims):
    return {
        "description": desc,
        "is_active": True,
        "is_diff_metric": False,
        "monitor_type": "metric",
        "name": name,
        "type": "double",
        "unit": unit,
        "dimensions": list(dims),
        "tag_list": [
            {"field_name": d, "unit": "--", "type": "string", "description": DIM_DESC[d]} for d in dims
        ],
    }


def group(table_name, table_desc, metrics, dim_names):
    fields = [metric(*m) for m in metrics] + [dim_field(d) for d in dim_names]
    return {
        "table_name": table_name,
        "table_desc": table_desc,
        "fields": fields,
        "rule_list": [],
        "dimensions": list(dim_names),
    }


CLUSTER = ["clusterName", "nodeName"]
QUEUE = ["clusterName", "nodeName", "vhostName", "queueName", "queueNode", "queueType"]
COLLECTOR_ERR = ["clusterName", "nodeName", "reason"]

overview = [
    ("rabbitmq_overview_messages", "集群消息总数", "none", CLUSTER),
    ("rabbitmq_overview_messages_ready", "集群 Ready", "none", CLUSTER),
    ("rabbitmq_overview_messages_unacked", "集群 Unacked", "none", CLUSTER),
    ("rabbitmq_overview_channels", "通道数", "none", CLUSTER),
    ("rabbitmq_overview_connections", "连接数", "none", CLUSTER),
    ("rabbitmq_overview_consumers", "消费者数", "none", CLUSTER),
    ("rabbitmq_overview_exchanges", "Exchange 数", "none", CLUSTER),
    ("rabbitmq_overview_queues", "队列数", "none", CLUSTER),
    ("rabbitmq_overview_messages_acked", "累计 ack", "none", CLUSTER),
    ("rabbitmq_overview_messages_delivered", "累计 deliver", "none", CLUSTER),
    ("rabbitmq_overview_messages_delivered_get", "累计 deliver+get", "none", CLUSTER),
    ("rabbitmq_overview_messages_published", "累计 publish", "none", CLUSTER),
    ("rabbitmq_overview_messages_ack_rate", "ack 速率", "none", CLUSTER),
    ("rabbitmq_overview_messages_deliver_rate", "deliver 速率", "none", CLUSTER),
    ("rabbitmq_overview_messages_publish_rate", "publish 速率", "none", CLUSTER),
    ("rabbitmq_overview_return_unroutable", "无法路由消息", "none", CLUSTER),
    ("rabbitmq_overview_return_unroutable_rate", "无法路由速率", "none", CLUSTER),
    ("rabbitmq_overview_clustering_listeners", "clustering listener", "none", CLUSTER),
    ("rabbitmq_overview_amqp_listeners", "amqp listener", "none", CLUSTER),
]

node = [
    ("rabbitmq_node_disk_free", "节点剩余磁盘", "bytes", CLUSTER),
    ("rabbitmq_node_disk_free_limit", "磁盘水位", "bytes", CLUSTER),
    ("rabbitmq_node_disk_free_alarm", "磁盘告警 0/1", "none", CLUSTER),
    ("rabbitmq_node_fd_used", "已用文件描述符", "none", CLUSTER),
    ("rabbitmq_node_fd_total", "文件描述符上限", "none", CLUSTER),
    ("rabbitmq_node_mem_used", "MQ 进程内存", "bytes", CLUSTER),
    ("rabbitmq_node_mem_limit", "内存上限", "bytes", CLUSTER),
    ("rabbitmq_node_mem_alarm", "内存告警 0/1", "none", CLUSTER),
    ("rabbitmq_node_proc_used", "Erlang 进程数", "none", CLUSTER),
    ("rabbitmq_node_proc_total", "Erlang 进程上限", "none", CLUSTER),
    ("rabbitmq_node_run_queue", "调度运行队列", "none", CLUSTER),
    ("rabbitmq_node_sockets_used", "已用 socket", "none", CLUSTER),
    ("rabbitmq_node_sockets_total", "socket 上限", "none", CLUSTER),
    ("rabbitmq_node_uptime", "节点运行时长", "ms", CLUSTER),
    ("rabbitmq_node_running", "节点运行中 0/1", "none", CLUSTER),
    ("rabbitmq_node_gc_num", "GC 次数", "none", CLUSTER),
    ("rabbitmq_node_gc_num_rate", "GC 速率", "none", CLUSTER),
    ("rabbitmq_node_io_read_avg_time", "磁盘读平均耗时", "none", CLUSTER),
    ("rabbitmq_node_io_write_avg_time", "磁盘写平均耗时", "none", CLUSTER),
]

queue = [
    ("rabbitmq_queue_messages", "队列消息总数", "none", QUEUE),
    ("rabbitmq_queue_messages_ready", "队列 Ready", "none", QUEUE),
    ("rabbitmq_queue_messages_unacked", "队列 Unacked", "none", QUEUE),
    ("rabbitmq_queue_consumers", "队列消费者", "none", QUEUE),
    ("rabbitmq_queue_consumer_utilisation", "消费者利用率 0-1", "none", QUEUE),
    ("rabbitmq_queue_memory", "队列内存", "bytes", QUEUE),
    ("rabbitmq_queue_message_bytes", "队列消息字节", "bytes", QUEUE),
    ("rabbitmq_queue_message_bytes_ready", "Ready 字节", "bytes", QUEUE),
    ("rabbitmq_queue_message_bytes_unacked", "Unacked 字节", "bytes", QUEUE),
    ("rabbitmq_queue_messages_publish", "队列累计 publish", "none", QUEUE),
    ("rabbitmq_queue_messages_publish_rate", "队列 publish 速率", "none", QUEUE),
    ("rabbitmq_queue_messages_deliver", "队列累计 deliver", "none", QUEUE),
    ("rabbitmq_queue_messages_deliver_rate", "队列 deliver 速率", "none", QUEUE),
    ("rabbitmq_queue_messages_ack", "队列累计 ack", "none", QUEUE),
    ("rabbitmq_queue_messages_ack_rate", "队列 ack 速率", "none", QUEUE),
    ("rabbitmq_queue_messages_redeliver", "队列累计 redeliver", "none", QUEUE),
    ("rabbitmq_queue_messages_redeliver_rate", "队列 redeliver 速率", "none", QUEUE),
    ("rabbitmq_queue_slave_nodes", "镜像从节点数", "none", QUEUE),
    ("rabbitmq_queue_synchronised_slave_nodes", "已同步从节点数", "none", QUEUE),
]

collector = [
    ("rabbitmq_collector_up", "采集成功 0/1", "none", CLUSTER),
    ("rabbitmq_collector_error", "采集失败 0/1", "none", COLLECTOR_ERR),
    ("rabbitmq_collector_local_node_found", "本机节点匹配成功 0/1", "none", CLUSTER),
    ("rabbitmq_collector_queue_series", "本机上报队列条数", "none", CLUSTER),
]

data = [
    {
        "table_name": "group_default",
        "table_desc": "默认分组",
        "fields": [],
        "rule_list": [],
        "dimensions": [],
    },
    group("overview", "集群总览", overview, CLUSTER),
    group("node", "节点资源", node, CLUSTER),
    group("queue", "队列明细", queue, QUEUE),
    group("collector", "采集自检", collector, ["clusterName", "nodeName", "reason"]),
]

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=4)
    f.write("\n")

print(f"wrote {OUT}")
for g in data:
    metrics = [x["name"] for x in g["fields"] if x.get("monitor_type") == "metric"]
    dims = [x["name"] for x in g["fields"] if x.get("monitor_type") == "dimension"]
    print(f"  {g['table_name']}: metrics={len(metrics)} dims={len(dims)}")
