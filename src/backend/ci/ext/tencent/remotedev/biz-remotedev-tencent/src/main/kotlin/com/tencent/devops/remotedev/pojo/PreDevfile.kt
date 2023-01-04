package com.tencent.devops.remotedev.pojo

import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileCommands
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfilePorts
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileVscode
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devfile pre定义处")
data class PreDevfile(
    @ApiModelProperty("定义devfile的版本")
    val version: String,
    @ApiModelProperty("用户指定的工作空间环境变量。")
    val envs: Map<String, String>?,
    @ApiModelProperty("定义用于工作区的docker镜像")
    /**
     * Example1. With a public image:
     *      image: ubuntu:latest
     *
     * Example2. With a custom image:
     *      image:
     *        file: ./.Dockerfile
     * @see DevfileImage
     */
    val image: Any?,
    @ApiModelProperty("配置vscode")
    val vscode: DevfileVscode?,
    @ApiModelProperty("配置需要监听的端口信息")
    val ports: List<DevfilePorts>?,
    @ApiModelProperty("用来指定工作空间声明周期命令")
    val commands: DevfileCommands?,
    @ApiModelProperty("DEVOPS_REMOTING_GIT_EMAIL 配置")
    val gitEmail: String?
)
