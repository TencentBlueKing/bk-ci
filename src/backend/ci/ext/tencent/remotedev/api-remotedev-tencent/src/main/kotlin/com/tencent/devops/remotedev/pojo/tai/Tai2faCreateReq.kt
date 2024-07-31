package com.tencent.devops.remotedev.pojo.tai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Moa2faReqData(
    val username: String,
    val channel: String,
    val language: String,
    val promptPayload: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Moa2faRespData(
    @JsonProperty("Ret")
    val ret: Int,
    @JsonProperty("ErrCode")
    val errCode: Int,
    @JsonProperty("ErrMsg")
    val errMsg: String,
    @JsonProperty("StackTrace")
    val stackTrace: String,
    @JsonProperty("Data")
    val data: CreateSessionData
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateSessionData(
    val sessionId: String
)
