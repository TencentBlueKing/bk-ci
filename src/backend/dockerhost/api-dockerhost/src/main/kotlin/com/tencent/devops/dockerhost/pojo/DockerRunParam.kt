package com.tencent.devops.dockerhost.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DockerRun")
data class DockerRunParam(
    @ApiModelProperty("镜像名称，包括tag", required = true)
    val imageName: String,
    @ApiModelProperty("镜像仓库用户名", required = true)
    val registryUser: String?,
    @ApiModelProperty("镜像仓库密码", required = true)
    val registryPwd: String?,
    @ApiModelProperty("命令行", required = false)
    val command: List<String>,
    @ApiModelProperty("环境变量", required = false)
    val env: Map<String, String?>?
)