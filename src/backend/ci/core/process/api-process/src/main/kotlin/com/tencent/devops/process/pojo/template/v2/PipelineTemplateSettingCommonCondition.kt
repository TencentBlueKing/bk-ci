package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板配置通用条件")
data class PipelineTemplateSettingCommonCondition(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String? = null,
    @get:Schema(title = "模板名称", required = true)
    val name: String? = null,
    @get:Schema(title = "模板配置版本", required = true)
    val settingVersion: Int? = null,
    @get:Schema(title = "模板ID与版本号对列表", required = false)
    val templateVersionPairs: List<TemplateVersionPair>? = emptyList(),
    @get:Schema(title = "创建人", required = true)
    val creator: String? = null,
    @get:Schema(title = "更新人", required = true)
    val updater: String? = null
)
