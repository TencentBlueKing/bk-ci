package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "PAC 流水线组创建请求")
data class PipelineYamlViewCreateReq(
    @get:Schema(title = "代码库 hashId", required = true)
    val repoHashId: String,
    @get:Schema(title = "代码库别名", required = true)
    val aliasName: String,
    @get:Schema(title = "需要创建的目录列表", required = true)
    val directoryList: Set<String>
)
