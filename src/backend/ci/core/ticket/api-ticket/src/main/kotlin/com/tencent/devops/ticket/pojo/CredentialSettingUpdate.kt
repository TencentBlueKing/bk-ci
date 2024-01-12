package com.tencent.devops.ticket.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "凭据-更新时内容")
data class CredentialSettingUpdate(
    @Schema(name = "凭据是否允许跨项目访问", required = true)
    val allowAcrossProject: Boolean
)
