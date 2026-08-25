package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import io.swagger.v3.oas.annotations.media.Schema

data class PTemplateTransferBody(
    @get:Schema(title = "流水线模板编排-model转yaml时必传", required = false)
    val templateModel: ITemplateModel?,
    @get:Schema(title = "流水线模板配置-model转yaml时必传", required = false)
    val templateSetting: PipelineSetting?,
    @get:Schema(title = "流水线入参-model转yaml时必传", required = true)
    val params: List<BuildFormProperty> = listOf(),
    @get:Schema(title = "流水线模板类型-model转yaml时必传", required = false)
    val templateType: PipelineTemplateType?,
    @get:Schema(title = "流水线模板YAML-yaml转model时必传", required = false)
    val yaml: String?
)
