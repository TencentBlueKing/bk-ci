package com.tencent.devops.dispatch.windows.pojo
import com.fasterxml.jackson.annotation.JsonProperty


data class WindowsMachineGetResponse(
    @JsonProperty("code")
    val code: Int, // 0
    @JsonProperty("data")
    val data: WindowsMachineGetResponseData,
    @JsonProperty("message")
    val message: String
)

data class WindowsMachineGetResponseData(
    @JsonProperty("taskGuid")
    val taskGuid: String // e8be308d-e0f7-46ab-9d40-6c4d63355a86
)