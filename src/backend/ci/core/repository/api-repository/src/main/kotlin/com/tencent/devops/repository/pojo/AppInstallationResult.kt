package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "代码库项目app安装结果")
data class AppInstallationResult(
    @Schema(name = "状态")
    val status: Boolean,
    @Schema(name = "url地址")
    val url: String = ""
)
