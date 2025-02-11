package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "授信设备信息")
data class TrustDeviceInfo(
    @get:Schema(title = "设备唯一标识")
    val deviceId: String,
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "更新时间")
    val updateTime: LocalDateTime,
    @get:Schema(title = "详细信息")
    val detail: TrustDeviceInfoDetail
)

@Schema(title = "获取授信设备Token数据")
data class TrustDeviceTokenGetData(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "设备唯一标识")
    val deviceId: String,
    @get:Schema(title = "详细信息")
    val detail: TrustDeviceInfoDetail
)

@Schema(title = "校验授信设备Token数据")
data class TrustDeviceTokenVerifyData(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "设备唯一标识")
    val deviceId: String,
    @get:Schema(title = "授信Token")
    val token: String
)

@Schema(title = "授信设备信息详细信息")
data class TrustDeviceInfoDetail(
    @get:Schema(title = "主机名")
    val hostname: String,
    @get:Schema(title = "操作系统")
    val os: String
)