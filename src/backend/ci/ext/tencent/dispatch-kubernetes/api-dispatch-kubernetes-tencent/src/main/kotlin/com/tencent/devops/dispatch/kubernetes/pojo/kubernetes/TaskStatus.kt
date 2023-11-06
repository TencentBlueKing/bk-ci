package com.tencent.devops.dispatch.kubernetes.pojo.kubernetes

data class TaskStatus(
    val uid: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val status: TaskStatusEnum? = null,
    val vmCreateResp: VmCreateResp? = null,
    val image: Image? = null,
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
    val curLaunchId: String,
    val macAddress: String? = ""
)

data class Image(
    val cosFile: String,
    val scene: String,
    val sourceEnv: String,
    val sourceCgsId: String,
    val sourceType: String,
    val size: String,
    val zoneId: String
)
