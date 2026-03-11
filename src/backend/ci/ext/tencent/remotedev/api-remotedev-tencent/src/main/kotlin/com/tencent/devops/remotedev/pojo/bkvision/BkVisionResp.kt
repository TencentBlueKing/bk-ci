package com.tencent.devops.remotedev.pojo.bkvision

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BkVisionResp(
    val result: Boolean?,
    val data: Any?,
    val code: Int?,
    val message: String?
)
