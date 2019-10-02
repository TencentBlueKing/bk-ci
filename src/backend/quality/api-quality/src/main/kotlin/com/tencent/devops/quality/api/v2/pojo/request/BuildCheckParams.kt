package com.tencent.devops.quality.api.v2.pojo.request

import io.swagger.annotations.ApiModel

@ApiModel("构建检查参数")
data class BuildCheckParams(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val buildNo: String,
    val startTime: Long,
    val taskId: String,
    val position: String
)