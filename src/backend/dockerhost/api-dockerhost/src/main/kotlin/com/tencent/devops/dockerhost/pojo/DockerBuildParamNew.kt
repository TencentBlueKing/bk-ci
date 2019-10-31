package com.tencent.devops.dockerhost.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DockerBuild")
data class DockerBuildParamNew(
    @ApiModelProperty("基础镜像凭证", required = true)
    val ticket: List<Triple<String, String, String>>,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("镜像TAG", required = true)
    val imageTag: String,
    @ApiModelProperty("构建目录", required = false)
    val buildDir: String? = ".",
    @ApiModelProperty("Dockerfile", required = false)
    val dockerFile: String? = "Dockerfile",
    @ApiModelProperty("repoAddr", required = true)
    val repoAddr: String,
    @ApiModelProperty("userName", required = true)
    val userName: String,
    @ApiModelProperty("password", required = true)
    val password: String,
    @ApiModelProperty("构建的参数", required = true)
    val args: List<String>,
    @ApiModelProperty("host配置", required = true)
    val host: List<String>

)