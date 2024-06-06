package com.tencent.devops.common.auth.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源授权DTO")
open class ResourceAuthorizationDTO(
    @get:Schema(title = "项目ID")
    open val projectCode: String,
    @get:Schema(title = "资源类型")
    open val resourceType: String,
    @get:Schema(title = "资源名称")
    open val resourceName: String,
    @get:Schema(title = "资源code")
    open val resourceCode: String,
    @get:Schema(title = "授权时间")
    open val handoverTime: Long? = null,
    @get:Schema(title = "授予人")
    open val handoverFrom: String? = null
)
