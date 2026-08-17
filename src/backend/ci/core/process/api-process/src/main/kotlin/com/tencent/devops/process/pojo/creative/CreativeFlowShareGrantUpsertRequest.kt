package com.tencent.devops.process.pojo.creative

import com.tencent.devops.process.enums.CreativeFlowShareScene
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享授权批量写入请求")
data class CreativeFlowShareGrantUpsertRequest(
    @get:Schema(title = "分享ID", required = true)
    val shareId: String,
    @get:Schema(title = "分享场景，本迭代仅 TALENT_FOLLOW")
    val scene: CreativeFlowShareScene = CreativeFlowShareScene.TALENT_FOLLOW,
    @get:Schema(title = "来源分身编码，仅审计与批量撤销")
    val talentCode: String? = null,
    @get:Schema(title = "授权条目列表", required = true)
    val flows: List<CreativeFlowShareGrantItem>
)
