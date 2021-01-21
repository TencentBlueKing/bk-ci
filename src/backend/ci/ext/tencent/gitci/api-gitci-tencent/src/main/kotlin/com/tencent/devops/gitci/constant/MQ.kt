package com.tencent.devops.gitci.constant

object MQ {

    // 工蜂CI请求
    const val EXCHANGE_GITCI_REQUEST_TRIGGER_EVENT = "e.gitci.request.trigger.event"
    const val ROUTE_GITCI_REQUEST_TRIGGER_EVENT = "r.gitci.request.trigger.event"
    const val QUEUE_GITCI_REQUEST_TRIGGER_EVENT = "q.gitci.request.trigger.event"

    // 工蜂Mr请求冲突检查
    const val EXCHANGE_GITCI_MR_CONFLICT_CHECK_EVENT = "e.gitci.mr.conflict.check.event"
    const val ROUTE_GITCI_MR_CONFLICT_CHECK_EVENT = "r.gitci.mr.conflict.check.event"
    const val QUEUE_GITCI_MR_CONFLICT_CHECK_EVENT = "q.gitci.mr.conflict.check.event"
}