package com.tencent.devops.dispatch.macos.pojo.devcloud

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DevCloud macOS VM Model 响应
 */
@Schema(title = "DevCloud macOS VM Model响应")
data class DevCloudMacosVmModelResponse(
    @get:Schema(title = "响应码")
    val actionCode: Int,
    @get:Schema(title = "响应消息")
    val actionMessage: String,
    @get:Schema(title = "数据列表")
    val data: List<DevCloudMacosVmModelData>?
)

/**
 * DevCloud macOS VM Model 数据项
 */
@Schema(title = "DevCloud macOS VM Model数据项")
data class DevCloudMacosVmModelData(
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "唯一标识")
    val uid: String,
    @get:Schema(title = "描述")
    val desc: String,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "镜像列表")
    val images: List<DevCloudMacosVmModelImage>?
)

/**
 * DevCloud macOS VM Model 镜像信息
 */
@Schema(title = "DevCloud macOS VM Model镜像信息")
data class DevCloudMacosVmModelImage(
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "唯一标识")
    val uid: String,
    @get:Schema(title = "描述")
    val desc: String,
    @get:Schema(title = "Xcode版本列表")
    val xcodes: List<String>?
)
