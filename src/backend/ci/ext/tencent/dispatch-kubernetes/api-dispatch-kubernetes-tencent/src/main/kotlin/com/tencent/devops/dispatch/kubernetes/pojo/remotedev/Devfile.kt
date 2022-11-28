package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devfile 定义处")
data class Devfile(
    @ApiModelProperty("定义在工作区的git配置键值对。")
    val gitConfig: Map<String, String>?,
    @ApiModelProperty("定义用于工作区的docker镜像")
    val image: DevfileImage?,
    @ApiModelProperty("配置vscode")
    val vscode: DevfileVscode?,
    @ApiModelProperty("配置需要监听的端口信息")
    val ports: List<DevfilePorts>?,
    @ApiModelProperty("项目准备工作")
    val postCreateCommand: String?
)

data class DevfileImage(
    @ApiModelProperty("定义公共镜像")
    val publicImage: String?,
    @ApiModelProperty("定义用户镜像")
    val file: String?
)

data class DevfileVscode(
    @ApiModelProperty("vscode 扩展")
    //  Open VSX?
    val extensions: List<String>?
)

data class DevfilePorts(
    @ApiModelProperty("端口名")
    val name: String?,
    @ApiModelProperty("端口号")
    val port: Int,
    @ApiModelProperty("描述")
    val description: String?,
    @ApiModelProperty("可见性")
    val visibility: String?
)
