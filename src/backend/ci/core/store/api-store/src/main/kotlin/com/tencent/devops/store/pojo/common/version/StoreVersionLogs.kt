package com.tencent.devops.store.pojo.common.version

import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "商店组件-版本日志")
data class StoreVersionLogs(
    @Schema(description = "总版本数")
    val totalVersions: Int,
    @Schema(description = "版本日志列表")
    val versions: List<StoreVersionLogInfo>?
)

