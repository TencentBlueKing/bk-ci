package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class BkUserInfo(
    @Schema(name = "用户Id")
    val id: Int,
    @Schema(name = "用户名")
    val username: String,
    @Schema(name = "是否启用")
    val enabled: Boolean,
    @Schema(name = "用户额外信息")
    val extras: BkUserExtras?,
    @Schema(name = "用户部门")
    val departments: List<BkUserDeptInfo>?
)
