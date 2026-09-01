package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享授权撤销请求")
data class CreativeFlowShareGrantRevokeRequest(
    @get:Schema(title = "分享ID；与 talentCode 二选一")
    val shareId: String? = null,
    @get:Schema(title = "按分享撤销时的条目ID列表")
    val flowIds: List<String>? = null,
    @get:Schema(title = "按分身批量撤销；与 shareId 二选一，必须有其一")
    val talentCode: String? = null
)
