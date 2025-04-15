package com.tencent.devops.remotedev.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceEstimateByVmResponse(
    val free: Int
)
