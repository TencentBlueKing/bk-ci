package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class FileSource(
    @get:Schema(title = "文件类型", description = "1-服务器文件，2-本地文件，3-文件源文件")
    val fileType: Int,
    @get:Schema(title = "文件路径列表")
    val fileList: List<String>,
    @get:Schema(title = "源文件所在机器")
    val server: VariableServer?,
    @get:Schema(title = "执行账号")
    val account: Account,
    @get:Schema(title = "第三方文件源ID")
    val fileSourceId: Long?,
    @get:Schema(title = "第三方文件源code")
    val fileSourceCode: String?
)