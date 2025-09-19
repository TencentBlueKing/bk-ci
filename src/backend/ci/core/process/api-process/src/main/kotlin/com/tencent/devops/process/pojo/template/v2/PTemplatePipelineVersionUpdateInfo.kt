package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模版流水线关联版本更新实体")
data class PTemplatePipelineVersionUpdateInfo(
    @get:Schema(title = "推荐版本号", required = false)
    val buildNo: BuildNo? = null,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null
)
