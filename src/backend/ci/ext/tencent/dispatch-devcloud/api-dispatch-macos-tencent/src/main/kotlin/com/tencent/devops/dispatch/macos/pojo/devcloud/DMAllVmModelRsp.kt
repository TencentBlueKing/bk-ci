package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DevCloud macOS VM Model 响应
 */
@Schema(title = "DevCloud macOS VM Model响应")
data class DMAllVmModelRsp(
    @get:Schema(title = "响应码")
    val actionCode: Int,
    @get:Schema(title = "响应消息")
    val actionMessage: String,
    @get:Schema(title = "数据列表")
    val data: DMAllVmModelData?
)

data class DMAllVmModelData(
    @get:Schema(title = "所有机型信息")
    val models: List<DevCloudMacosVmModelData>,
    @get:Schema(title = "所有镜像信息")
    val images: List<DevCloudMacosVmModelImage>
)