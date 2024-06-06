package com.tencent.devops.common.auth.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源授权返回体")
class ResourceAuthorizationResponse(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源名称")
    val resourceName: String,
    @get:Schema(title = "资源code")
    val resourceCode: String,
    @get:Schema(title = "授权时间")
    val handoverTime: Long,
    @get:Schema(title = "授予人")
    val handoverFrom: String
)
