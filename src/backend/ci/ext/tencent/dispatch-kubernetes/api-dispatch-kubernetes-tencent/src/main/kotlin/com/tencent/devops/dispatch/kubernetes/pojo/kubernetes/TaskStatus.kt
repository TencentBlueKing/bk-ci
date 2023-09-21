package com.tencent.devops.dispatch.kubernetes.pojo.kubernetes

data class TaskStatus(
    val uid: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val status: TaskStatusEnum? = null,
    val vmCreateResp: VmCreateResp? = null,
    val logs: List<String> = emptyList()
)

enum class TaskStatusEnum {
    waiting,
    running,
    successed,
    failed,
    abort
}

data class VmCreateResp(
    val cgsIp: String,
    val cloudZoneId: String,
    val existed: Boolean,
    val resourceId: String,
    val envId: String? = "",
    val curLaunchId: String
)
