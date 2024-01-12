package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "代码库项目app安装结果")
data class AppInstallationResult(
    @Schema(description = "状态")
    val status: Boolean,
    @Schema(description = "url地址")
    val url: String = ""
)
