package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板复制创建请求体")
data class PipelineTemplateCopyCreateReq(
    @get:Schema(title = "源模板ID", required = true)
    val srcTemplateId: String,
    @get:Schema(title = "源模板版本", required = true)
    val srcTemplateVersion: Long? = null,
    @get:Schema(title = "是否复制配置", required = true)
    val copySetting: Boolean,
    @get:Schema(title = "模板名称", required = true)
    val name: String
) : PipelineTemplateVersionReq
