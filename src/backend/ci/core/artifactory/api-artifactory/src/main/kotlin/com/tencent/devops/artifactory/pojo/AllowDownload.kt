package com.tencent.devops.artifactory.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "版本仓库-是否允许下载")
data class AllowDownload(
    @get:Schema(title = "是否允许下载", required = true)
    val allowDownload: Boolean,
    @get:Schema(title = "错误信息", required = true)
    val errorMsg: String
)
