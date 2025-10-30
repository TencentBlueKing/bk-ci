package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 获取桌面缩略图请求
 */
@Schema(title = "获取桌面缩略图请求")
data class FetchDesktopThumbnailReq(
    @get:Schema(title = "用户ID", required = true)
    @JsonProperty("user_id")
    val userId: String,

    @get:Schema(title = "CDS实例ID", required = true)
    @JsonProperty("cds_id")
    val cdsId: String,

    @get:Schema(title = "缩略图宽度", required = true)
    @JsonProperty("width")
    val width: Int,

    @get:Schema(title = "缩略图高度", required = true)
    @JsonProperty("high")
    val high: Int,

    @get:Schema(title = "截图序列ID", required = true)
    @JsonProperty("screen_id")
    val screenId: Int,

    @get:Schema(title = "JPEG图片质量", required = true)
    @JsonProperty("jpeg_quality")
    val jpegQuality: Int,

    @get:Schema(title = "JPEG图片URL", required = true)
    @JsonProperty("jpeg_url")
    val jpegUrl: String
)
