package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "源文件信息")
data class FileSource(
    @get:Schema(title = "文件列表", required = true)
    val fileList: List<String>,
    @get:Schema(title = "源文件服务器", required = true)
    val sourceFileServer: ExecuteTarget,
    @get:Schema(title = "文件源账号", required = true)
    val account: Account
)