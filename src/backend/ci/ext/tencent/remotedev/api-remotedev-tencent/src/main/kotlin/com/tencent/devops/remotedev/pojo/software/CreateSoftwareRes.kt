package com.tencent.devops.remotedev.pojo.software

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallSoftwareRes(
    val result: Boolean,
    val message: String,
    val data: DataRes
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataRes(
    @JsonProperty("task_id")
    val taskId: Long
)
