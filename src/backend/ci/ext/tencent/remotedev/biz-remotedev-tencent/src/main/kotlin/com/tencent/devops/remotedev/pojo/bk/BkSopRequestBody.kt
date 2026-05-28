package com.tencent.devops.remotedev.pojo.bk

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class BkSopRequestBody(
    val name: String,
    val constants: Map<String, String>,
    val scope: String = "project"
)

data class BkSopResponse(
    val result: Boolean,
    val data: Data?
) {
    data class Data(
        @JsonProperty("task_id")
        val taskId: Int,
        @JsonProperty("task_url")
        val taskUrl: String
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BkSopStatusResp(
    val state: String
)