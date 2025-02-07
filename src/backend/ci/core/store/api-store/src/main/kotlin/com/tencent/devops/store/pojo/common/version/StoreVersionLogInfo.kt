package com.tencent.devops.store.pojo.common.version

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "商店组件-版本日志")
data class StoreVersionLogInfo(
    @Schema(description = "版本号")
    val version: String,
    @Schema(description = "tag")
    val tag: String?,
    @Schema(description = "最近更新时间")
    val lastUpdateTime: String,
    @Schema(description = "更新日志")
    val updateLog: String?

)
