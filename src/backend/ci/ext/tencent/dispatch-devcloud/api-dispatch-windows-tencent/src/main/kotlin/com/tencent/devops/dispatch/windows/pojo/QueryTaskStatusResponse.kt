package com.tencent.devops.dispatch.windows.pojo
import com.fasterxml.jackson.annotation.JsonProperty


data class QueryTaskStatusResponse(
    @JsonProperty("code")
    val code: Int, // 200
    @JsonProperty("data")
    val data: QueryTaskStatusResponseData,
    @JsonProperty("message")
    val message: String
)

data class QueryTaskStatusResponseData(
    @JsonProperty("buildTime")
    val buildTime: String, // 25s
    @JsonProperty("createdAt")
    val createdAt: String, // 2018-03-23 10:26:18
    @JsonProperty("result")
    val result: QueryTaskStatusResponseResult?,
    @JsonProperty("status")
    val status: String, // succeeded
    @JsonProperty("updatedAt")
    val updatedAt: String // 2018-03-23 10:26:37
)

data class QueryTaskStatusResponseResult(
    @JsonProperty("ip")
    val ip: String, // 1.1.1.1
    @JsonProperty("processId")
    val processId: String, // sdsf
    @JsonProperty("status")
    val status: String // deleted
)