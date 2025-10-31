package com.tencent.devops.common.auth.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "全量资源授权交接条件实体")
data class ResetAllResourceAuthorizationReq(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "授予人")
    val handoverFrom: String? = null,
    @get:Schema(title = "授予人列表")
    val handoverFroms: List<String>? = emptyList(),
    @get:Schema(title = "交接人")
    val handoverTo: String?,
    @get:Schema(title = "是否为预检查，若为true,不做权限交接")
    val preCheck: Boolean = true,
    @get:Schema(title = "是否校验操作人权限")
    val checkPermission: Boolean = true
)
