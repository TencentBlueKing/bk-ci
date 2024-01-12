package com.tencent.devops.common.api.pojo.agent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "docker init 文件升级信息")
data class DockerInitFileInfo(
    @Schema(description = "文件md5值")
    val fileMd5: String,
    @Schema(description = "目前只支持linux机器，所以其他系统不需要检查")
    val needUpgrade: Boolean
)
