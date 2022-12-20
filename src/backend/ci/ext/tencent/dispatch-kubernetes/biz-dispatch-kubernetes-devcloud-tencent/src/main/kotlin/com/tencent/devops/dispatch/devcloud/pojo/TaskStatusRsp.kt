package com.tencent.devops.dispatch.devcloud.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.TaskStatus

data class TaskStatusRsp(
    val data: TaskStatus,
    val code: Int,
    val message: String
)
