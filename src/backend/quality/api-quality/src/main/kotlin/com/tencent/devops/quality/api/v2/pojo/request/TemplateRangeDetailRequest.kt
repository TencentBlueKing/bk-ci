package com.tencent.devops.quality.api.v2.pojo.request

import io.swagger.annotations.ApiModel

@ApiModel("流水线模板范围请求")
data class TemplateRangeDetailRequest(
    val projectId: String,
    val templateIds: Set<String>,
    val indicatorIds: Collection<String>,
    val controlPointType: String?
)