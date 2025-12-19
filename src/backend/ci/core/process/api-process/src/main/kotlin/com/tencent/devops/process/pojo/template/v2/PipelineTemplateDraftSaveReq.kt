package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板更新请求体")
data class PipelineTemplateDraftSaveReq(
    @get:Schema(title = "草稿源版本，若为空，默认最新版本", required = true)
    var baseVersion: Long? = null,
    @get:Schema(title = "logo地址", required = false)
    val logoUrl: String? = null,
    @get:Schema(title = "是否开启PAC", required = false)
    val enablePac: Boolean? = null,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>? = null,
    @get:Schema(title = "存储格式", required = true)
    val storageType: PipelineStorageType = PipelineStorageType.MODEL,
    @get:Schema(title = "原始编排-storageType为MODEL时，必传", required = false)
    val model: ITemplateModel? = null,
    @get:Schema(title = "模板配置-storageType为MODEL时，必传", required = false)
    val templateSetting: PipelineSetting? = null,
    @get: Schema(title = "模板类型-storageType为MODEL时，必传", required = false)
    val type: PipelineTemplateType? = null,
    @get: Schema(title = "编排yaml-storageType为YAML时，必传", required = false)
    val yaml: String? = null
) : PipelineTemplateVersionReq
