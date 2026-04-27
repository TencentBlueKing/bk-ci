package com.tencent.devops.remotedev.pojo.cvd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CVD资源池用户信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class CvdPoolUser(
    @get:Schema(description = "用户名")
    val username: String,
    @get:Schema(description = "用户类型")
    val userType: String? = null
)
