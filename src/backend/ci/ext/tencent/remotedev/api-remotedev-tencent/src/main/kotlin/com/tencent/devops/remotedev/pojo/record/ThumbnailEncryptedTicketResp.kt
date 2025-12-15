package com.tencent.devops.remotedev.pojo.record

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "缩略图加密密钥响应")
data class ThumbnailEncryptedTicketResp(
    @get:Schema(title = "工作空间名称", required = true)
    val workspaceName: String,
    @get:Schema(title = "AI数字孪生标识", required = true)
    val aiDigitalTwinFlag: Boolean,
    @get:Schema(title = "加密密钥", required = true)
    val cryptKey: String? = null
)
