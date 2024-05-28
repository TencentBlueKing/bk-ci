package com.tencent.devops.common.auth.callback

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "过滤条件搜索实例")
data class FetchInstanceListData(
    val projectId: String,
    val projectName: String,
    val pipelineId: String,
    val pipelineName: String
)
