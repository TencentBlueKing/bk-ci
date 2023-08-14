package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentResourceDataRsp(
    val code: Int,
    val data: List<EnvironmentResourceData>?,
    val message: String
)
