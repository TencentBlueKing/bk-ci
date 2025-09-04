package com.tencent.devops.remotedev.pojo.record

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "是否开启录屏以及推流地址")
data class CheckWorkspaceRecordData(
    @get:Schema(title = "是否开启录屏", required = true)
    val openRecord: Boolean,
    @get:Schema(title = "推流地址", required = false)
    val recordAddress: String?
)
