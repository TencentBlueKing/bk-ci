package com.tencent.devops.remotedev.pojo.startcloud

import io.swagger.v3.oas.annotations.media.Schema

/**
 * CDS截图上传通知请求（批量）
 */
@Schema(title = "CDS截图上传通知请求")
data class ScreenshotUploadNotifyRequest(
    @get:Schema(title = "应用名称", required = true)
    val appName: String,
    @get:Schema(title = "BkRepo上传地址映射（cgsId -> uploadUrl）", required = true)
    val uploadUrls: Map<String, String>
)
