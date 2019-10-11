package com.tencent.devops.quality.api.v2.pojo.request

import io.swagger.annotations.ApiModel

@ApiModel("流水线范围请求")
data class PipelineRangeDetailRequest(
    val projectId: String,
    val pipelineIds: Set<String>,
    val indicatorIds: Collection<String>,
    val controlPointType: String?
)