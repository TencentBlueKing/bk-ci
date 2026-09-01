package com.tencent.devops.process.service.task.copy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线复制资源基础信息")
data class PipelineCopyResourceBasicInfo(
    val resourceId: String,
    val resourceName: String
)
