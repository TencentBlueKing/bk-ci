package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.api.v2.pojo.response.RangeExistElement
import io.swagger.annotations.ApiModel

@ApiModel("流水线模板生效范围")
data class RuleTemplateRange(
    val templateId: String,
    val templateName: String,
    val elementCount: Int,
    val lackPointElement: Collection<String>,
    val existElement: Collection<RangeExistElement>
)