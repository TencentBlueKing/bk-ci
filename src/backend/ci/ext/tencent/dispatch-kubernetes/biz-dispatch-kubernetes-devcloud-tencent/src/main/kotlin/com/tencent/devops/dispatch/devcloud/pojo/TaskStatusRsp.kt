package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.kubernetes.TaskStatus

data class TaskStatusRsp(
    val data: TaskStatus,
    val code: Int,
    val message: String
)
