package com.tencent.devops.process.pojo.template

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import io.swagger.v3.oas.annotations.media.Schema

data class TemplatePreviewDetail(
    @get:Schema(title = "模板模型")
    val template: Model,
    @get:Schema(title = "模板Yaml")
    val templateYaml: String?,
    @get:Schema(title = "是否有操作权限", required = false)
    var hasPermission: Boolean,
    @get:Schema(title = "预览流水线设置")
    val setting: PipelineSetting?,
    @get:Schema(title = "高亮位置，可能多个")
    val highlightMarkList: List<TransferMark>?,
    @get:Schema(title = "是否支持YAML解析", required = true)
    val yamlSupported: Boolean = true,
    @get:Schema(title = "YAML解析异常信息")
    val yamlInvalidMsg: String? = null
)
