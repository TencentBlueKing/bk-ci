package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class BkUserInfo(
    @Schema(title = "用户Id")
    val id: Int,
    @Schema(title = "用户名")
    val username: String,
    @Schema(title = "是否启用")
    val enabled: Boolean,
    @Schema(title = "用户额外信息")
    val extras: BkUserExtras?,
    @Schema(title = "用户部门")
    val departments: List<BkUserDeptInfo>?
)
