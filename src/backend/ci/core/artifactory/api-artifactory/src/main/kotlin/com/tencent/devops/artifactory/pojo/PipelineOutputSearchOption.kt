package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.PipelineOutputType
import io.swagger.v3.oas.annotations.media.Schema

data class PipelineOutputSearchOption(
    val pipelineOutputType: PipelineOutputType?,
    @get:Schema(title = "查询元数据", required = false)
    val qualityMetadata: List<Property> = emptyList()
)
