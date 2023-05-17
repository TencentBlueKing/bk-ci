package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.PipelineOutputType

data class PipelineOutputSearchOption(
    val pipelineOutputType: PipelineOutputType?
)
