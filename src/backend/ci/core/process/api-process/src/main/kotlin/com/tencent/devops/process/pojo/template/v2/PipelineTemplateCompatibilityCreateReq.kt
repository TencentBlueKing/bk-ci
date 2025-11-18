package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板兼容创建请求体")
data class PipelineTemplateCompatibilityCreateReq(
    val model: Model,
    val setting: PipelineSetting,
    val v1VersionName: String = "init",
    val category: String? = null,
    val logoUrl: String? = null
) : PipelineTemplateVersionReq
