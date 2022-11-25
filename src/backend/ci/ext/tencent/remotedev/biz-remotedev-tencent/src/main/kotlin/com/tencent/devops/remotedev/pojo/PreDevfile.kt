package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devfile pre定义处")
data class PreDevfile(
    @ApiModelProperty("定义在工作区的git配置键值对。")
    val gitConfig: Map<String, String>?,
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
    @ApiModelProperty("项目准备工作")
    val postCreateCommand: String?
)
