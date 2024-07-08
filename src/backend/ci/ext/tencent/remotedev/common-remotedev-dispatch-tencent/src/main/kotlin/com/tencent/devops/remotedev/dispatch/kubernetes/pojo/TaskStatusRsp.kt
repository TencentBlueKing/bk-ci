package com.tencent.devops.remotedev.dispatch.kubernetes.pojo

import com.tencent.devops.remotedev.pojo.kubernetes.TaskStatus

data class TaskStatusRsp(
    val data: TaskStatus,
    val code: Int,
    val message: String
)
