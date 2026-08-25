package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享授权条目（写入）")
data class CreativeFlowShareGrantItem(
    @get:Schema(title = "创作流条目ID", required = true)
    val flowId: String,
    @get:Schema(title = "源项目ID", required = true)
    val sourceProjectId: String,
    @get:Schema(title = "源流水线ID", required = true)
    val sourcePipelineId: String,
    @get:Schema(title = "发布版本号，形如 V208；缺省表示授权最新已发布版本")
    val versionNum: String? = null,
    @get:Schema(title = "扩展信息")
    val extInfo: CreativeFlowShareExtInfo? = null
)
