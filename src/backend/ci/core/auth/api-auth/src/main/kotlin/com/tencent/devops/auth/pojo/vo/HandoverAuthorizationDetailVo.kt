package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.enum.HandoverType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "授权交接详细表")
data class HandoverAuthorizationDetailVo(
    @get:Schema(title = "授权资源ID")
    val resourceCode: String,
    @get:Schema(title = "授权资源名称")
    val resourceName: String,
    @get:Schema(title = "交接类型")
    val handoverType: HandoverType,
    @get:Schema(title = "授权人")
    val handoverFrom: String
)
