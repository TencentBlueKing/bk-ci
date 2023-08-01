package com.tencent.devops.remotedev.pojo.software

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONPropertyIgnore
import org.json.JSONPropertyName

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateSoftwareRes(
    val result: String,
    val message: String,
    val data: DataRes
)
data class DataRes(
    @JsonProperty("task_id")
    val taskId: String
)
