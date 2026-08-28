#!/usr/bin/env bash
# devops_rabbitmq_plugin — 蓝鲸脚本插件采集（对标 Telegraf inputs.rabbitmq）
#
# 插件：devops_rabbitmq_plugin（is_split_measurement=true，enable_field_blacklist=true）
# 未在「指标维度」里登记的指标会被丢掉。导入：
#   docs/monitor/devops_rabbitmq_plugin_metrics.json
# 分组会变成结果表：
#   script_devops_rabbitmq_plugin.overview / .node / .queue / .collector
#
# 输出：metric{clusterName,nodeName,...} value
# 面板用维度拆集群/vhost/队列，采集脚本不按环境分叉。
# 调试前把 PASS 改成 telegraf 口令，否则只会出 collector_error。
#
# 周期：60s    依赖：curl、jq    Management：本机 15672
#
# 节点上可选覆盖密码（各集群口令不同时）：
#   /data/bkmonitor/bkci_rabbitmq.env
#   BKCI_RABBITMQ_USER=telegraf
#   BKCI_RABBITMQ_PASS=...
#
# 环境变量（均可在 env 文件或插件脚本顶部改）：
#   QUEUE_SCOPE=owner|all     默认 owner：本节点只报自己持有/leader 的队列，避免三台各报全量
#   NODE_SCOPE=local|all      默认 local：本节点只报自己的 rabbitmq_node
#   SKIP_EXCLUSIVE=1          跳过 exclusive 临时队列
#   SKIP_AUTODELETE=1         跳过 auto_delete 临时队列
#   COLLECT_EXCHANGE=0        默认不采 exchange（基数大且值班价值低）
#
# 指标（对标 telegraf rabbitmq_overview / rabbitmq_node / rabbitmq_queue）：
#   rabbitmq_overview_*   维度 clusterName,nodeName
#   rabbitmq_node_*       维度 clusterName,nodeName
#   rabbitmq_queue_*      维度 clusterName,nodeName,vhostName,queueName,queueNode,queueType
#   rabbitmq_collector_*  采集器自检

set -euo pipefail
export LC_ALL=C
export LANG=C

for f in \
    "${BKCI_RABBITMQ_ENV:-}" \
    /data/bkmonitor/bkci_rabbitmq.env \
    /etc/bkmonitor/bkci_rabbitmq.env
do
    if [[ -n "${f}" && -f "${f}" ]]; then
        # shellcheck disable=SC1090
        source "${f}"
        break
    fi
done

HOST="${BKCI_RABBITMQ_HOST:-127.0.0.1}"
PORT="${BKCI_RABBITMQ_PORT:-15672}"
USER="${BKCI_RABBITMQ_USER:-telegraf}"
# 粘贴到插件时改成现网 telegraf 口令；不要把真实密码提交进 git。
PASS="${BKCI_RABBITMQ_PASS:-CHANGE_ME}"

QUEUE_SCOPE="${QUEUE_SCOPE:-owner}"
NODE_SCOPE="${NODE_SCOPE:-local}"
SKIP_EXCLUSIVE="${SKIP_EXCLUSIVE:-1}"
SKIP_AUTODELETE="${SKIP_AUTODELETE:-1}"
COLLECT_EXCHANGE="${COLLECT_EXCHANGE:-0}"
CURL_TIMEOUT="${CURL_TIMEOUT:-25}"

need() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "rabbitmq_collector_up{clusterName=\"unknown\", nodeName=\"unknown\"} 0"
        echo "rabbitmq_collector_error{clusterName=\"unknown\", nodeName=\"unknown\", reason=\"missing_$1\"} 1"
        exit 0
    }
}
need curl
need jq

if [[ "${PASS}" == "CHANGE_ME" || -z "${PASS}" ]]; then
    echo "rabbitmq_collector_up{clusterName=\"unknown\", nodeName=\"unknown\"} 0"
    echo "rabbitmq_collector_error{clusterName=\"unknown\", nodeName=\"unknown\", reason=\"password_unset\"} 1"
    exit 0
fi

AUTH="${USER}:${PASS}"
API="http://${HOST}:${PORT}/api"

api_get() {
    curl -sS --max-time "${CURL_TIMEOUT}" --user "${AUTH}" \
        -H "content-type:application/json" --url "$1" || true
}

OVERVIEW_JSON="$(api_get "${API}/overview")"
NODES_JSON="$(api_get "${API}/nodes")"
QUEUES_JSON="$(api_get "${API}/queues")"

if ! jq -e '.queue_totals and .object_totals' >/dev/null 2>&1 <<<"${OVERVIEW_JSON}"; then
    echo "rabbitmq_collector_up{clusterName=\"unknown\", nodeName=\"unknown\"} 0"
    echo "rabbitmq_collector_error{clusterName=\"unknown\", nodeName=\"unknown\", reason=\"overview_auth_or_down\"} 1"
    exit 0
fi

CLUSTER_NAME="$(jq -r '.cluster_name // empty' <<<"${OVERVIEW_JSON}")"
if [[ -z "${CLUSTER_NAME}" ]]; then
    CLUSTER_NAME="$(jq -r '[.[].name] | sort | join(",")' <<<"${NODES_JSON}")"
fi
[[ -n "${CLUSTER_NAME}" ]] || CLUSTER_NAME="unknown"

SHORT_HOST="$(hostname -s 2>/dev/null || hostname)"
FQDN_HOST="$(hostname 2>/dev/null || echo "${HOSTNAME:-}")"
LOCAL_NODE="$(
    jq -r --arg h "${HOSTNAME:-}" --arg s "${SHORT_HOST}" --arg f "${FQDN_HOST}" '
        def candidates: ["rabbit@"+$h, "rabbit@"+$s, "rabbit@"+$f];
        (candidates | map(select(length > 7)) | unique) as $c
        | ( [.[] | select(.name as $n | $c | index($n) != null)] | .[0].name // empty )
        as $exact
        | if $exact != "" then $exact
          else ([.[] | select(.name | test($s))] | .[0].name // empty)
          end
    ' <<<"${NODES_JSON}"
)"
[[ -n "${LOCAL_NODE}" ]] || LOCAL_NODE="rabbit@${SHORT_HOST}"

JQ_COMMON='
def n: if . == null then 0 else . end;
def f: if . == null then 0 else . end;
def b: if . == true then 1 else 0 end;
def esc: tostring | gsub("\\\\"; "\\\\") | gsub("\""; "\\\"");
def tags(obj):
  (obj | to_entries | map("\(.key)=\"\(.value | esc)\"") | join(", "));
def line(metric; obj; val):
  "\(metric){\(tags(obj))} \(val)";
'

run_jq() {
    local input="$1"
    local body="$2"
    shift 2
    jq -r "$@" "${JQ_COMMON}${body}" <<<"${input}"
}

COMMON_TAGS_JSON="$(jq -nc --arg c "${CLUSTER_NAME}" --arg n "${LOCAL_NODE}" \
    '{clusterName:$c, nodeName:$n}')"

# --- overview：集群总量，每台都会报同一份，面板按 clusterName 做 MAX，不要按 IP SUM ---
run_jq "${OVERVIEW_JSON}" '
    . as $o
    | ($o.listeners // []) as $ls
    | ($t) as $tags
    | line("rabbitmq_overview_messages"; $tags; $o.queue_totals.messages | n),
      line("rabbitmq_overview_messages_ready"; $tags; $o.queue_totals.messages_ready | n),
      line("rabbitmq_overview_messages_unacked"; $tags; $o.queue_totals.messages_unacknowledged | n),
      line("rabbitmq_overview_channels"; $tags; $o.object_totals.channels | n),
      line("rabbitmq_overview_connections"; $tags; $o.object_totals.connections | n),
      line("rabbitmq_overview_consumers"; $tags; $o.object_totals.consumers | n),
      line("rabbitmq_overview_exchanges"; $tags; $o.object_totals.exchanges | n),
      line("rabbitmq_overview_queues"; $tags; $o.object_totals.queues | n),
      line("rabbitmq_overview_messages_acked"; $tags; ($o.message_stats.ack | n)),
      line("rabbitmq_overview_messages_delivered"; $tags; ($o.message_stats.deliver | n)),
      line("rabbitmq_overview_messages_delivered_get"; $tags; ($o.message_stats.deliver_get | n)),
      line("rabbitmq_overview_messages_published"; $tags; ($o.message_stats.publish | n)),
      line("rabbitmq_overview_messages_ack_rate"; $tags; ($o.message_stats.ack_details.rate | f)),
      line("rabbitmq_overview_messages_deliver_rate"; $tags; ($o.message_stats.deliver_details.rate | f)),
      line("rabbitmq_overview_messages_publish_rate"; $tags; ($o.message_stats.publish_details.rate | f)),
      line("rabbitmq_overview_return_unroutable"; $tags; ($o.message_stats.return_unroutable | n)),
      line("rabbitmq_overview_return_unroutable_rate"; $tags; ($o.message_stats.return_unroutable_details.rate | f)),
      line("rabbitmq_overview_clustering_listeners"; $tags; ([$ls[] | select(.protocol=="clustering")] | length)),
      line("rabbitmq_overview_amqp_listeners"; $tags; ([$ls[] | select(.protocol=="amqp")] | length))
' --argjson t "${COMMON_TAGS_JSON}"

# --- node：默认只报本机，对应 telegraf rabbitmq_node ---
run_jq "${NODES_JSON}" '
    .[]
    | select($scope == "all" or .name == $local)
    | . as $n
    | {clusterName:$cluster, nodeName:$n.name} as $tags
    | line("rabbitmq_node_disk_free"; $tags; $n.disk_free | n),
      line("rabbitmq_node_disk_free_limit"; $tags; $n.disk_free_limit | n),
      line("rabbitmq_node_disk_free_alarm"; $tags; $n.disk_free_alarm | b),
      line("rabbitmq_node_fd_used"; $tags; $n.fd_used | n),
      line("rabbitmq_node_fd_total"; $tags; $n.fd_total | n),
      line("rabbitmq_node_mem_used"; $tags; $n.mem_used | n),
      line("rabbitmq_node_mem_limit"; $tags; $n.mem_limit | n),
      line("rabbitmq_node_mem_alarm"; $tags; $n.mem_alarm | b),
      line("rabbitmq_node_proc_used"; $tags; $n.proc_used | n),
      line("rabbitmq_node_proc_total"; $tags; $n.proc_total | n),
      line("rabbitmq_node_run_queue"; $tags; $n.run_queue | n),
      line("rabbitmq_node_sockets_used"; $tags; $n.sockets_used | n),
      line("rabbitmq_node_sockets_total"; $tags; $n.sockets_total | n),
      line("rabbitmq_node_uptime"; $tags; $n.uptime | n),
      line("rabbitmq_node_running"; $tags; $n.running | b),
      line("rabbitmq_node_gc_num"; $tags; $n.gc_num | n),
      line("rabbitmq_node_gc_num_rate"; $tags; ($n.gc_num_details.rate | f)),
      line("rabbitmq_node_io_read_avg_time"; $tags; ($n.io_read_avg_time | f)),
      line("rabbitmq_node_io_write_avg_time"; $tags; ($n.io_write_avg_time | f))
' --arg cluster "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}" --arg scope "${NODE_SCOPE}"

# --- queue：对标 telegraf rabbitmq_queue，不按 vhost 拆指标名 ---
QUEUE_COUNT="$(
    run_jq "${QUEUES_JSON}" '
        [
          .[]
          | select($skip_excl != "1" or (.exclusive | not))
          | select($skip_ad != "1" or (.auto_delete | not))
          | select(
              $queue_scope == "all"
              or .node == $local
              or ((.leader // "") == $local)
            )
        ] | length
    ' --arg local "${LOCAL_NODE}" --arg queue_scope "${QUEUE_SCOPE}" \
        --arg skip_excl "${SKIP_EXCLUSIVE}" --arg skip_ad "${SKIP_AUTODELETE}"
)"

run_jq "${QUEUES_JSON}" '
    .[]
    | select($skip_excl != "1" or (.exclusive | not))
    | select($skip_ad != "1" or (.auto_delete | not))
    | select(
        $queue_scope == "all"
        or .node == $local
        or ((.leader // "") == $local)
      )
    | . as $q
    | {
        clusterName: $cluster,
        nodeName: $local,
        vhostName: ($q.vhost // ""),
        queueName: ($q.name // ""),
        queueNode: ($q.node // $q.leader // ""),
        queueType: ($q.type // "classic")
      } as $tags
    | line("rabbitmq_queue_messages"; $tags; $q.messages | n),
      line("rabbitmq_queue_messages_ready"; $tags; $q.messages_ready | n),
      line("rabbitmq_queue_messages_unacked"; $tags; $q.messages_unacknowledged | n),
      line("rabbitmq_queue_consumers"; $tags; $q.consumers | n),
      line("rabbitmq_queue_consumer_utilisation"; $tags; $q.consumer_utilisation | f),
      line("rabbitmq_queue_memory"; $tags; $q.memory | n),
      line("rabbitmq_queue_message_bytes"; $tags; $q.message_bytes | n),
      line("rabbitmq_queue_message_bytes_ready"; $tags; $q.message_bytes_ready | n),
      line("rabbitmq_queue_message_bytes_unacked"; $tags; $q.message_bytes_unacknowledged | n),
      line("rabbitmq_queue_messages_publish"; $tags; ($q.message_stats.publish | n)),
      line("rabbitmq_queue_messages_publish_rate"; $tags; ($q.message_stats.publish_details.rate | f)),
      line("rabbitmq_queue_messages_deliver"; $tags; ($q.message_stats.deliver | n)),
      line("rabbitmq_queue_messages_deliver_rate"; $tags; ($q.message_stats.deliver_details.rate | f)),
      line("rabbitmq_queue_messages_ack"; $tags; ($q.message_stats.ack | n)),
      line("rabbitmq_queue_messages_ack_rate"; $tags; ($q.message_stats.ack_details.rate | f)),
      line("rabbitmq_queue_messages_redeliver"; $tags; ($q.message_stats.redeliver | n)),
      line("rabbitmq_queue_messages_redeliver_rate"; $tags; ($q.message_stats.redeliver_details.rate | f)),
      line("rabbitmq_queue_slave_nodes"; $tags; (($q.slave_nodes // []) | length)),
      line("rabbitmq_queue_synchronised_slave_nodes"; $tags; (($q.synchronised_slave_nodes // []) | length))
' --arg cluster "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}" \
    --arg queue_scope "${QUEUE_SCOPE}" --arg skip_excl "${SKIP_EXCLUSIVE}" \
    --arg skip_ad "${SKIP_AUTODELETE}"

if [[ "${COLLECT_EXCHANGE}" == "1" ]]; then
    EXCHANGE_JSON="$(api_get "${API}/exchanges")"
    run_jq "${EXCHANGE_JSON}" '
        .[]
        | select((.name // "") != "" and ((.name | startswith("amq.")) | not))
        | . as $e
        | {
            clusterName: $cluster,
            nodeName: $local,
            vhostName: ($e.vhost // ""),
            exchangeName: ($e.name // ""),
            exchangeType: ($e.type // "")
          } as $tags
        | line("rabbitmq_exchange_publish_in"; $tags; ($e.message_stats.publish_in | n)),
          line("rabbitmq_exchange_publish_in_rate"; $tags; ($e.message_stats.publish_in_details.rate | f)),
          line("rabbitmq_exchange_publish_out"; $tags; ($e.message_stats.publish_out | n)),
          line("rabbitmq_exchange_publish_out_rate"; $tags; ($e.message_stats.publish_out_details.rate | f))
    ' --arg cluster "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}"
fi

LOCAL_FOUND=0
if jq -e --arg n "${LOCAL_NODE}" '.[] | select(.name==$n)' >/dev/null 2>&1 <<<"${NODES_JSON}"; then
    LOCAL_FOUND=1
fi

jq -n -r --argjson t "${COMMON_TAGS_JSON}" --argjson up 1 --argjson found "${LOCAL_FOUND}" \
    --argjson queues "${QUEUE_COUNT:-0}" "${JQ_COMMON}
    line(\"rabbitmq_collector_up\"; \$t; \$up),
    line(\"rabbitmq_collector_local_node_found\"; \$t; \$found),
    line(\"rabbitmq_collector_queue_series\"; \$t; \$queues)
"
