package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class StartCloudComputerStatusReqBody(
    val appName: String,
    val cgsIds: Set<String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudComputerStatusRespData(
    val cgsId: String,
    val state: Int,
    val message: String?,
    val userInfos: List<StartCloudComputerStatusUserInfo>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudComputerStatusUserInfo(
    val account: String
)
