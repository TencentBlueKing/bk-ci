package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentDefaltRsp(
    val code: Int,
    val message: String
)
