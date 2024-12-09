package com.tencent.devops.remotedev.pojo.record

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询录屏元数据参数")
data class FetchMetaDataParam(
    val userId: String,
    val projectId: String,
    val workspaceName: String,
    val page: Int?,
    val pageSize: Int?,
    val startTime: Long,
    val stopTime: Long
)
