#!/usr/bin/env bash
# devops_rabbitmq_plugin — 蓝鲸脚本插件（bkmonitor.woa.com / 业务 100205）
# 对标 Telegraf inputs.rabbitmq：采集侧不分集群，面板用维度切换。
#
# 输出：metric{cluster="rbac-core", clusterName="...", nodeName="...", ...} value
# cluster 是业务集群名（优先本机 IP，其次 hostname 前缀），clusterName 是 RabbitMQ 原名。
#
# 业务集群名跟 Management URL 对齐（不要用节点名里的 rbac/ext 子串）：
#   auto&stream  /mq/auto/  bkdevops_core_mq_   30.186.110.10  30.186.110.65  30.189.37.13
#   rbac         /mq/rbac/  bkdevops_rbac_mq_   21.200.250.146 21.200.250.152 21.200.250.34
#   vip          /mq/vip/   mq-node             30.186.111.174 30.186.110.252 30.186.110.74
#   ext          /mq/ext/   bkdevops_mq_mr_     9.136.132.205  9.136.162.33   9.136.161.63
# 不要匹配 bkdevops_ext_rbac_mq_*（采集里有，但不属于上述四个 Management）。
#
# 插件 enable_field_blacklist=true 时必须导入：
#   docs/monitor/devops_rabbitmq_plugin_metrics.json
# 结果表：script_devops_rabbitmq_plugin.{overview,node,vhost,queue,exchange,collector}
#
# 调试前把 PASS 改成 telegraf 口令，否则只有 collector_error。
# 各集群口令不同时，节点放 /data/bkmonitor/bkci_rabbitmq.env

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
PASS="${BKCI_RABBITMQ_PASS:-CHANGE_ME}"

# 尽量多采：全量队列 + vhost + exchange；exclusive 也采，面板再滤 exclusive=false
QUEUE_SCOPE="${QUEUE_SCOPE:-all}"
NODE_SCOPE="${NODE_SCOPE:-local}"
SKIP_EXCLUSIVE="${SKIP_EXCLUSIVE:-0}"
SKIP_AUTODELETE="${SKIP_AUTODELETE:-0}"
COLLECT_EXCHANGE="${COLLECT_EXCHANGE:-1}"
COLLECT_VHOST="${COLLECT_VHOST:-1}"
COLLECT_NODE_MEMORY="${COLLECT_NODE_MEMORY:-1}"
CURL_TIMEOUT="${CURL_TIMEOUT:-25}"

fail() {
    local reason="$1"
    echo "rabbitmq_collector_up{cluster=\"unknown\", clusterName=\"unknown\", nodeName=\"unknown\"} 0"
    echo "rabbitmq_collector_error{cluster=\"unknown\", clusterName=\"unknown\", nodeName=\"unknown\", reason=\"${reason}\"} 1"
    exit 0
}

command -v curl >/dev/null 2>&1 || fail "missing_curl"
command -v jq >/dev/null 2>&1 || fail "missing_jq"
[[ "${PASS}" != "CHANGE_ME" && -n "${PASS}" ]] || fail "password_unset"

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
    fail "overview_auth_or_down"
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
        | ([.[] | select(.name as $n | $c | index($n) != null)] | .[0].name // empty) as $exact
        | if $exact != "" then $exact
          else ([.[] | select(.name | test($s))] | .[0].name // empty)
          end
    ' <<<"${NODES_JSON}"
)"
[[ -n "${LOCAL_NODE}" ]] || LOCAL_NODE="rabbit@${SHORT_HOST}"

host_ips() {
    {
        hostname -I 2>/dev/null || true
        ip -4 -o addr show 2>/dev/null | awk '{print $4}' | cut -d/ -f1 || true
    } | tr ' ' '\n' | awk 'NF && $1 != "127.0.0.1"'
}

# 跟 /mq/{auto,rbac,vip,ext}/ 对齐。节点名里的 rbac/ext 子串会误伤，IP 与精确前缀优先。
derive_cluster() {
    if [[ -n "${CLUSTER_LABEL:-}" ]]; then
        echo "${CLUSTER_LABEL}"
        return
    fi
    local ips
    ips=" $(host_ips | tr '\n' ' ') "
    case "${ips}" in
        *" 30.186.110.10 "*|*" 30.186.110.65 "*|*" 30.189.37.13 "*)
            echo "auto&stream"; return ;;
        *" 21.200.250.146 "*|*" 21.200.250.152 "*|*" 21.200.250.34 "*)
            echo "rbac"; return ;;
        *" 30.186.111.174 "*|*" 30.186.110.252 "*|*" 30.186.110.74 "*)
            echo "vip"; return ;;
        *" 9.136.132.205 "*|*" 9.136.162.33 "*|*" 9.136.161.63 "*)
            echo "ext"; return ;;
    esac
    local s
    s="$(printf '%s %s %s' "${SHORT_HOST}" "${CLUSTER_NAME}" "${LOCAL_NODE}" | tr '[:upper:]' '[:lower:]')"
    case "${s}" in
        *bkdevops_ext_rbac*) echo "ignore-ext-rbac" ;;
        *bkdevops_rbac_mq*) echo "rbac" ;;
        *bkdevops_core_mq*) echo "auto&stream" ;;
        *mq-node*) echo "vip" ;;
        *bkdevops_mq_mr*) echo "ext" ;;
        *) echo "${CLUSTER_NAME}" ;;
    esac
}
CLUSTER="$(derive_cluster)"

JQ_COMMON='
def n: if . == null then 0 else . end;
def f: if . == null then 0 else . end;
def b: if . == true then 1 else 0 end;
def flag: if . == true then "true" else "false" end;
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

BASE_TAGS="$(jq -nc --arg c "${CLUSTER}" --arg cn "${CLUSTER_NAME}" --arg n "${LOCAL_NODE}" \
    '{cluster:$c, clusterName:$cn, nodeName:$n}')"

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
' --argjson t "${BASE_TAGS}"

run_jq "${NODES_JSON}" '
    .[]
    | select($scope == "all" or .name == $local)
    | . as $n
    | {cluster:$cluster, clusterName:$cname, nodeName:$n.name} as $tags
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
      line("rabbitmq_node_io_write_avg_time"; $tags; ($n.io_write_avg_time | f)),
      line("rabbitmq_node_partitions"; $tags; (($n.partitions // []) | length))
' --arg cluster "${CLUSTER}" --arg cname "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}" --arg scope "${NODE_SCOPE}"

if [[ "${COLLECT_NODE_MEMORY}" == "1" ]]; then
    NODE_LIST="$(jq -r --arg local "${LOCAL_NODE}" --arg scope "${NODE_SCOPE}" \
        '.[] | select($scope == "all" or .name == $local) | .name' <<<"${NODES_JSON}")"
    while IFS= read -r nname; do
        [[ -n "${nname}" ]] || continue
        enc="$(jq -rn --arg n "${nname}" '$n | @uri')"
        mem_json="$(api_get "${API}/nodes/${enc}/memory")"
        run_jq "${mem_json}" '
            (.memory // .) as $m
            | {cluster:$cluster, clusterName:$cname, nodeName:$node} as $tags
            | line("rabbitmq_node_mem_connection_readers"; $tags; $m.connection_readers | n),
              line("rabbitmq_node_mem_connection_writers"; $tags; $m.connection_writers | n),
              line("rabbitmq_node_mem_queue_procs"; $tags; $m.queue_procs | n),
              line("rabbitmq_node_mem_plugins"; $tags; $m.plugins | n),
              line("rabbitmq_node_mem_mgmt_db"; $tags; $m.mgmt_db | n),
              line("rabbitmq_node_mem_binary"; $tags; $m.binary | n),
              line("rabbitmq_node_mem_allocated"; $tags; (
                  if ($m.total | type) == "object" then ($m.total.allocated // $m.total.rss // 0)
                  else ($m.total // 0) end | n))
        ' --arg cluster "${CLUSTER}" --arg cname "${CLUSTER_NAME}" --arg node "${nname}"
    done <<<"${NODE_LIST}"
fi

if [[ "${COLLECT_VHOST}" == "1" ]]; then
    VHOSTS_JSON="$(api_get "${API}/vhosts")"
    run_jq "${VHOSTS_JSON}" '
        .[]
        | . as $v
        | {cluster:$cluster, clusterName:$cname, nodeName:$local, vhostName:($v.name // "")} as $tags
        | line("rabbitmq_vhost_messages"; $tags; $v.messages | n),
          line("rabbitmq_vhost_messages_ready"; $tags; $v.messages_ready | n),
          line("rabbitmq_vhost_messages_unacked"; $tags; $v.messages_unacknowledged | n)
    ' --arg cluster "${CLUSTER}" --arg cname "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}"
fi

QUEUE_COUNT="$(
    run_jq "${QUEUES_JSON}" '
        [
          .[]
          | select($skip_excl != "1" or (.exclusive | not))
          | select($skip_ad != "1" or (.auto_delete | not))
          | select($queue_scope == "all" or .node == $local or ((.leader // "") == $local))
        ] | length
    ' --arg local "${LOCAL_NODE}" --arg queue_scope "${QUEUE_SCOPE}" \
        --arg skip_excl "${SKIP_EXCLUSIVE}" --arg skip_ad "${SKIP_AUTODELETE}"
)"

run_jq "${QUEUES_JSON}" '
    .[]
    | select($skip_excl != "1" or (.exclusive | not))
    | select($skip_ad != "1" or (.auto_delete | not))
    | select($queue_scope == "all" or .node == $local or ((.leader // "") == $local))
    | . as $q
    | {
        cluster: $cluster,
        clusterName: $cname,
        nodeName: $local,
        vhostName: ($q.vhost // ""),
        queueName: ($q.name // ""),
        queueNode: ($q.node // $q.leader // ""),
        queueType: ($q.type // "classic"),
        exclusive: ($q.exclusive | flag),
        durable: ($q.durable | flag)
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
' --arg cluster "${CLUSTER}" --arg cname "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}" \
    --arg queue_scope "${QUEUE_SCOPE}" --arg skip_excl "${SKIP_EXCLUSIVE}" --arg skip_ad "${SKIP_AUTODELETE}"

if [[ "${COLLECT_EXCHANGE}" == "1" ]]; then
    EXCHANGE_JSON="$(api_get "${API}/exchanges")"
    run_jq "${EXCHANGE_JSON}" '
        .[]
        | select((.name // "") != "" and ((.name | startswith("amq.")) | not))
        | . as $e
        | {
            cluster: $cluster,
            clusterName: $cname,
            nodeName: $local,
            vhostName: ($e.vhost // ""),
            exchangeName: ($e.name // ""),
            exchangeType: ($e.type // "")
          } as $tags
        | line("rabbitmq_exchange_publish_in"; $tags; ($e.message_stats.publish_in | n)),
          line("rabbitmq_exchange_publish_in_rate"; $tags; ($e.message_stats.publish_in_details.rate | f)),
          line("rabbitmq_exchange_publish_out"; $tags; ($e.message_stats.publish_out | n)),
          line("rabbitmq_exchange_publish_out_rate"; $tags; ($e.message_stats.publish_out_details.rate | f))
    ' --arg cluster "${CLUSTER}" --arg cname "${CLUSTER_NAME}" --arg local "${LOCAL_NODE}"
fi

LOCAL_FOUND=0
if jq -e --arg n "${LOCAL_NODE}" '.[] | select(.name==$n)' >/dev/null 2>&1 <<<"${NODES_JSON}"; then
    LOCAL_FOUND=1
fi

jq -n -r --argjson t "${BASE_TAGS}" --argjson up 1 --argjson found "${LOCAL_FOUND}" \
    --argjson queues "${QUEUE_COUNT:-0}" "${JQ_COMMON}
    line(\"rabbitmq_collector_up\"; \$t; \$up),
    line(\"rabbitmq_collector_local_node_found\"; \$t; \$found),
    line(\"rabbitmq_collector_queue_series\"; \$t; \$queues)
"
