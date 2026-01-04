package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板商店导入创建请求体")
data class PipelineTemplateMarketCreateReq(
    @get:Schema(title = "研发商店模板项目ID", required = true)
    val marketTemplateProjectId: String,
    @get:Schema(title = "研发商店模板ID", required = true)
    val marketTemplateId: String,
    @get:Schema(title = "研发商店模板版本", required = true)
    val marketTemplateVersion: Long? = null,
    @get:Schema(title = "是否复制配置", required = true)
    val copySetting: Boolean = false,
    @get:Schema(title = "模板名称", required = true)
    val name: String? = null
) : PipelineTemplateVersionReq
