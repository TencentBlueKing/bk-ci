package com.tencent.devops.common.api.pojo.agent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("docker init 文件升级信息")
data class DockerInitFileInfo(
    @ApiModelProperty("文件md5值")
    val fileMd5: String,
    @ApiModelProperty("目前只支持linux机器，所以其他系统不需要检查")
    val needUpgrade: Boolean
)
