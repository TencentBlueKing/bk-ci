package com.tencent.devops.common.auth.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源授权返回体")
@Suppress("LongParameterList")
data class ResourceAuthorizationResponse(
    @get:Schema(title = "ID")
    val id: Long? = null,
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
    val handoverFrom: String,
    @get:Schema(title = "授予人中文名称")
    val handoverFromCnName: String? = null,
    @get:Schema(title = "是否有执行权限")
    val executePermission: Boolean? = null,
    @get:Schema(title = "是否正在交接，用于我的授权界面")
    val beingHandover: Boolean? = null,
    @get:Schema(title = "交接审批人")
    val approver: String? = null
)
