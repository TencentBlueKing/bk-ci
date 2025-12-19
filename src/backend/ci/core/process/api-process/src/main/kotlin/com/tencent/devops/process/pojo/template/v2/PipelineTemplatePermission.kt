package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板自定义创建请求体")
open class PipelineTemplatePermission(
    @get:Schema(title = "项目ID", required = true)
    open val projectId: String,
    @get:Schema(title = "模板id", required = true)
    open val id: String,
    @get:Schema(title = "模板名称", required = true)
    open val name: String,
    @get:Schema(title = "创建人", required = true)
    open val creator: String
)
