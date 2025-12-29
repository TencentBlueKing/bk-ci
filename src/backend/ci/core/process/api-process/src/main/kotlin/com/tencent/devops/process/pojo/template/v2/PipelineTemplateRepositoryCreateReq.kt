package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板商店导入创建请求体")
data class PipelineTemplateRepositoryCreateReq(
    @get:Schema(title = "代码库哈希Id", required = true)
    val repoHashId: String,
    @get:Schema(title = "默认分支", required = true)
    val branch: String,
    @get:Schema(title = "模板文件名称列表", required = true)
    val fileNames: List<String>
) : PipelineTemplateVersionReq
