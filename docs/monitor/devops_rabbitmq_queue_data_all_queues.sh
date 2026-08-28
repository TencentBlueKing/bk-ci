#!/usr/bin/env bash
# 蓝盾 RabbitMQ 全量队列采集脚本（替换指标插件 devops_rabbitmq_queue_data 的 linux 脚本）
# 输出格式：metric_name{label="value"} number
#
# 相对现网脚本的变化：
# 1. 仍上报 nodeTotalMessagesValue / nodeMemory*，告警策略不用改
# 2. 仍上报原来 4 条硬编码队列的 per-vhost utilisation，旧图不断
# 3. 新增通用指标：queueMessages/Ready/Unacked/Consumers/ConsumerUtilisation + queueName/vhostName
# 4. 一次 GET /api/queues，覆盖集群全部非 exclusive 队列
#
# 插件侧还要新增这 5 个指标字段（维度 queueName、vhostName），然后升级采集 1131。
# 密码沿用插件里现有配置，不要把口令写进仓库。

export LC_ALL=C
export LANG=C

host=127.0.0.1
port=15672
user="telegraf"
# 粘贴到指标插件时，改成现网脚本里的同一组账号，不要把口令提交进 git。
pass="${RABBITMQ_PASS:-CHANGE_ME}"

auth="${user}:${pass}"
api="http://${host}:${port}/api"

legacy_queues=(
    engine.pipeline.build.task.start.engine-taskBuildConsumer
    engine.pipeline.agent.startup.dispatch-devcloud-startDevCloudConsumer
    build.log.origin.event.log-logOriginEventConsumer
    build.log.storage.event.log-logStorageEventConsumer
)

totalMessages=$(curl -s --request GET --user "${auth}" -H "content-type:application/json" \
    --url "${api}/overview" | jq -r '.queue_totals.messages')
if [[ "${totalMessages}" != "null" && -n "${totalMessages}" ]]; then
    echo "nodeTotalMessagesValue{nodeTotalMessages=\"${totalMessages}\"} ${totalMessages}"
fi

memoryUseByte=$(curl -s --request GET --user "${auth}" -H "content-type:application/json" \
    --url "${api}/nodes/rabbit@${HOSTNAME}" | jq -r '.mem_used')
if [[ "${memoryUseByte}" != "null" && -n "${memoryUseByte}" ]]; then
    memoryUseMb=$(echo "scale=2; a = ${memoryUseByte} / 1024 / 1024 ; if (length(a)==scale(a)) print 0;print a" | bc)
    memoryUseGb=$(echo "scale=2; a = ${memoryUseByte} / 1024 / 1024 / 1024 ; if (length(a)==scale(a)) print 0;print a" | bc)
    echo "nodeMemoryUseByteValue{nodeMemoryUseByte=\"${memoryUseByte}\"} ${memoryUseByte}"
    echo "nodeMemoryUseMbValue{nodeMemoryUseMb=\"${memoryUseMb}\"} ${memoryUseMb}"
    echo "nodeMemoryUseGbValue{nodeMemoryUseGb=\"${memoryUseGb}\"} ${memoryUseGb}"
fi

# 跳过 exclusive / auto_delete 临时队列，避免 fanout 消费者队列把基数打爆。
# 若要字面意义上的全部 900+ 队列，删掉 select 条件即可。
curl -s --request GET --user "${auth}" -H "content-type:application/json" \
    --url "${api}/queues" \
    | jq -r --argjson legacy "$(printf '%s\n' "${legacy_queues[@]}" | jq -R . | jq -s .)" '
        def esc: gsub("\\\\"; "\\\\") | gsub("\""; "\\\"");
        def vhost_metric:
          if . == "bkdevops-rbac" then "queueBkdevopsRbacConsumerUtilisationValue"
          elif . == "rbac-k8s" then "queueRbacK8sConsumerUtilisationValue"
          elif . == "rbac-core" then "queueRbacCoreConsumerUtilisationValue"
          elif . == "rbac-gray-core" then "queueRbacGrayCoreConsumerUtilisationValue"
          elif . == "auto-core" then "queueAutoCoreConsumerUtilisationValue"
          elif . == "stream-core" then "queueStreamCoreConsumerUtilisationValue"
          elif . == "stream-gray-core" then "queueStreamGrayCoreConsumerUtilisationValue"
          elif . == "rbac-gray-ext" then "queueRbacGrayExtConsumerUtilisationValue"
          elif . == "stream-ext" then "queueStreamExtConsumerUtilisationValue"
          elif . == "stream-gray-ext" then "queueStreamGrayExtConsumerUtilisationValue"
          elif . == "auto-ext" then "queueAutoExtCoreConsumerUtilisationValue"
          elif . == "rbac-ext" then "queueRbacExtConsumerUtilisationValue"
          else "queueUnknownConsumerUtilisationValue"
          end;
        .[]
        | select((.exclusive | not) and (.auto_delete | not))
        | . as $q
        | ($q.name | esc) as $name
        | ($q.vhost | esc) as $vhost
        | "queueMessagesValue{queueName=\"\($name)\", vhostName=\"\($vhost)\"} \($q.messages // 0)",
          "queueReadyValue{queueName=\"\($name)\", vhostName=\"\($vhost)\"} \($q.messages_ready // 0)",
          "queueUnackedValue{queueName=\"\($name)\", vhostName=\"\($vhost)\"} \($q.messages_unacknowledged // 0)",
          "queueConsumersValue{queueName=\"\($name)\", vhostName=\"\($vhost)\"} \($q.consumers // 0)",
          "queueConsumerUtilisationValue{queueName=\"\($name)\", vhostName=\"\($vhost)\"} \($q.consumer_utilisation // 0)",
          (if ($legacy | index($q.name)) != null
           then "\($q.vhost | vhost_metric){queueName=\"\($name)\", vhostName=\"\($vhost)\"} \($q.consumer_utilisation // 0)"
           else empty end)
      '
