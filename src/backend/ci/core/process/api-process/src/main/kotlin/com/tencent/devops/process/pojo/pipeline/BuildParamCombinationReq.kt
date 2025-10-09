package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.web.annotation.BkField
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线启动参数组合请求")
data class BuildParamCombinationReq(
    @get:Schema(description = "组合名称")
    @field:BkField(minLength = 1, maxLength = 64)
    val combinationName: String,
    @get:Schema(description = "参数")
    val params: List<BuildFormProperty>
)
