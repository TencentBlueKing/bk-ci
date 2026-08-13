package com.tencent.devops.process.pojo.creative

import com.tencent.devops.process.enums.CreativeFlowShareGrantStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareScene
import com.tencent.devops.process.enums.CreativeFlowShareVersionScope
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流分享授权")
data class CreativeFlowShareGrantVo(
    val shareId: String,
    val flowId: String,
    val scene: CreativeFlowShareScene,
    @get:Schema(title = "分享形态，当前恒为 COPY")
    val shareMode: CreativeFlowShareMode,
    val sourceProjectId: String,
    val sourcePipelineId: String,
    val versionScope: CreativeFlowShareVersionScope,
    @get:Schema(title = "发布版本号，形如 V208；LATEST 时为空")
    val versionNum: String? = null,
    val validateRules: CreativeFlowShareValidateRules? = null,
    val status: CreativeFlowShareGrantStatus,
    val talentCode: String? = null,
    val grantedBy: String,
    val grantedTime: Long,
    val extInfo: CreativeFlowShareExtInfo? = null
)
