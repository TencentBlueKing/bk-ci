package com.tencent.devops.process.pojo.creative

import com.tencent.devops.process.enums.CreativeFlowCopyStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareScene
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流复制溯源记录")
data class CreativeFlowCopyTraceVo(
    val id: Long,
    val shareId: String,
    val flowId: String,
    val scene: CreativeFlowShareScene,
    val shareMode: CreativeFlowShareMode,
    val talentCode: String? = null,
    val sourceProjectId: String,
    val sourcePipelineId: String,
    val sourceVersion: Int,
    val sourceVersionNum: String? = null,
    val targetProjectId: String,
    val targetPipelineId: String,
    val targetPipelineName: String,
    val targetVersion: Int,
    val targetVersionNum: String? = null,
    val targetEnvHashId: String? = null,
    val copyAction: CreativeFlowCopyStatus,
    val variableOverrides: String? = null,
    val operator: String,
    val createTime: Long
)
