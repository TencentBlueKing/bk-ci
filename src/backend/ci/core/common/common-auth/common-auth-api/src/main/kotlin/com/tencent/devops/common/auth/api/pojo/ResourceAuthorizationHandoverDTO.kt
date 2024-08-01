package com.tencent.devops.common.auth.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "交接资源授权DTO")
data class ResourceAuthorizationHandoverDTO(
    @get:Schema(title = "项目ID")
    override val projectCode: String,
    @get:Schema(title = "资源类型")
    override val resourceType: String,
    @get:Schema(title = "资源名称")
    override val resourceName: String,
    @get:Schema(title = "资源code")
    override val resourceCode: String,
    @get:Schema(title = "授予时间")
    override val handoverTime: Long? = null,
    @get:Schema(title = "授予人")
    override val handoverFrom: String? = null,
    @get:Schema(title = "授予人中文名称")
    override var handoverFromCnName: String? = null,
    @get:Schema(title = "交接人")
    val handoverTo: String? = null,
    @get:Schema(title = "交接人")
    val handoverToCnName: String? = null,
    @get:Schema(title = "交接失败信息")
    val handoverFailedMessage: String? = null
) : ResourceAuthorizationDTO(
    projectCode = projectCode,
    resourceType = resourceType,
    resourceName = resourceName,
    resourceCode = resourceCode,
    handoverFrom = handoverFrom
)
