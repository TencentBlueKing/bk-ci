package com.tencent.devops.remotedev.pojo.tai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CertExchangeCodeReq(
    val username: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CertExchangeCodeResp(
    val code: Int,
    val msg: String,
    val data: CertExchangeCodeData?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CertExchangeCodeData(
    val code: String
)
