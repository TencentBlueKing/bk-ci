package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.api.v2.pojo.response.RangeExistElement
import io.swagger.annotations.ApiModel

@ApiModel("流水线生效范围")
data class RulePipelineRange(
    val pipelineId: String,
    val pipelineName: String,
    val elementCount: Int,
    val lackPointElement: Collection<String>,
    val existElement: Collection<RangeExistElement>
)