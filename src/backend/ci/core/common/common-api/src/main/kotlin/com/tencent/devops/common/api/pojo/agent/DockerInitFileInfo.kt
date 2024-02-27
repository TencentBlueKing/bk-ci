package com.tencent.devops.common.api.pojo.agent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "docker init 文件升级信息")
data class DockerInitFileInfo(
    @get:Schema(title = "文件md5值")
    val fileMd5: String,
    @get:Schema(title = "目前只支持linux机器，所以其他系统不需要检查")
    val needUpgrade: Boolean
)
