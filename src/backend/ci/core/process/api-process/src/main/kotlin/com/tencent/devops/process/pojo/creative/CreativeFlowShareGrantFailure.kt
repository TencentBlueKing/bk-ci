package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享授权写入失败项")
data class CreativeFlowShareGrantFailure(
    val flowId: String,
    val errorCode: String,
    val message: String
)
