package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享授权批量写入结果")
data class CreativeFlowShareGrantUpsertResult(
    val granted: List<CreativeFlowShareGrantVo>,
    val failed: List<CreativeFlowShareGrantFailure>
)
