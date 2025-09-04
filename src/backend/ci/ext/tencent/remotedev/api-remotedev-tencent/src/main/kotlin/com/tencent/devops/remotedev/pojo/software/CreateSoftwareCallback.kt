package com.tencent.devops.remotedev.pojo.software

import com.fasterxml.jackson.annotation.JsonProperty

data class SoftwareCallbackRes(
    @JsonProperty("task_id")
    val taskId: Long,
    @JsonProperty("task_status")
    val taskStatus: TaskStatusEnum,
    @JsonProperty("software_status_info")
    val softwareStatusInfo: Map<String, String?>
)

enum class TaskStatusEnum {
    FINISHED,
    FAILED,
    RUNNING,
    SUSPENDED,
    REVOKED,
    WAITING
}
