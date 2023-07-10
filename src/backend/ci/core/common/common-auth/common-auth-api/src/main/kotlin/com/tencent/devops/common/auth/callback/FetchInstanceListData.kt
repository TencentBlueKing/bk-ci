package com.tencent.devops.common.auth.callback

import io.swagger.annotations.ApiModel

@ApiModel("过滤条件搜索实例")
data class FetchInstanceListData(
    val projectId: String,
    val projectName: String,
    val pipelineId: String,
    val pipelineName: String
)
