package com.tencent.devops.common.remotedev

object MQ {
    const val EXCHANGE_WORKSPACE_UPDATE_FROM_K8S = "e.engine.remotedev.workspace.listener"
    const val ROUTE_WORKSPACE_UPDATE_FROM_K8S = "r.engine.remotedev.workspace.listener.update"
    const val QUEUE_WORKSPACE_UPDATE_FROM_K8S = "q.engine.remotedev.workspace.listener.update"

    // 远程开发工作空间 ====================================
    const val EXCHANGE_REMOTE_DEV_LISTENER_DIRECT = "e.engine.remotedev.workspace.listener"
    const val ROUTE_WORKSPACE_CREATE_STARTUP = "r.engine.remotedev.workspace.listener.create"
    const val QUEUE_WORKSPACE_CREATE_STARTUP = "q.engine.remotedev.workspace.listener.create"
    const val ROUTE_WORKSPACE_OPERATE_STARTUP = "r.engine.remotedev.workspace.listener.operate"
    const val QUEUE_WORKSPACE_OPERATE_STARTUP = "q.engine.remotedev.workspace.listener.operate"
}
