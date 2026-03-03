package com.tencent.devops.process.pojo.pipeline.version

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线指定版本回滚请求")
data class PipelineRollbackDraftReq(
    val draftVersion: Int? = null
) : PipelineVersionCreateReq
