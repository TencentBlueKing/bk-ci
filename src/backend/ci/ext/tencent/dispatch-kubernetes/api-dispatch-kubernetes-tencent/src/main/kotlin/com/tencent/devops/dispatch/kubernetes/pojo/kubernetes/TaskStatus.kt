package com.tencent.devops.dispatch.kubernetes.pojo.kubernetes

import com.fasterxml.jackson.annotation.JsonProperty

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
    val curLaunchId: String
)

data class Image(
    @JsonProperty("cos_file")
    val cosFile: String,
    val scene: String,
    @JsonProperty("source_env")
    val sourceEnv: String,
    @JsonProperty("source_cgsid")
    val sourceCgsId: String,
    @JsonProperty("source_type")
    val sourceType: String
)
