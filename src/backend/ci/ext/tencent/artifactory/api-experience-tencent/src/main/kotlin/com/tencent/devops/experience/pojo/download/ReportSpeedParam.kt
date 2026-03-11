package com.tencent.devops.experience.pojo.download

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "上报下载速度参数")
data class ReportSpeedParam(
    @get:Schema(title = "体验ID", required = false)
    val experienceId: String,
    @get:Schema(title = "下载速度", required = true)
    val downloadSpeed: Long,
    @get:Schema(title = "下载类型,1--服务器下载 , 2--P2P下载", required = true)
    val downloadType: Int
)
