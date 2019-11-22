package com.tencent.devops.gitci.listener

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.common.ci.yaml.CIBuildYaml

@Event(MQ.EXCHANGE_GITCI_REQUEST_TRIGGER_EVENT, MQ.ROUTE_GITCI_REQUEST_TRIGGER_EVENT)
data class GitCIRequestTriggerEvent(
    val event: GitRequestEvent,
    val yaml: CIBuildYaml
)
