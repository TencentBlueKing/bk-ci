package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库项目app安装结果")
data class AppInstallationResult(
    @get:Schema(title = "状态")
    val status: Boolean,
    @get:Schema(title = "url地址")
    val url: String = ""
)
