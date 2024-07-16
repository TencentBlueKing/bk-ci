package com.tencent.devops.common.remotedev

object RemoteDevMQ {
    // 远程开发工作空间 ====================================
    // dispatch-k8s生产 / remote dev消费
    const val WORKSPACE_UPDATE_FROM_K8S = "remotedev.workspace.listener.update"
    // remote dev生产 / dispatch-k8s消费
    const val WORKSPACE_CREATE_STARTUP = "remotedev.workspace.listener.create"
    // remote dev生产 / dispatch-k8s消费
    const val WORKSPACE_OPERATE_STARTUP = "remotedev.workspace.listener.operate"

    const val REMOTE_DEV_ASYNC_EXECUTE = "remotedev.async.execute"
}
