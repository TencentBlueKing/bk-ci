package com.tencent.devops.remotedev.pojo.tai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Moa2faVerifyReqData(
    val sessionId: String,
    val username: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Moa2faVerifyRespData(
    @JsonProperty("Ret")
    val ret: Int,
    @JsonProperty("ErrCode")
    val errCode: Int,
    @JsonProperty("ErrMsg")
    val errMsg: String,
    @JsonProperty("StackTrace")
    val stackTrace: String,
    @JsonProperty("Data")
    val data: VerifySessionData
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VerifySessionData(
    val sessionId: String,
    val status: String
)
