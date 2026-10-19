package com.tencent.devops.process.engine.dao.creative

import com.tencent.devops.process.enums.CreativeFlowCopyStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareScene

data class CreativeFlowCopyTrace(
    val id: Long? = null,
    val shareId: String,
    val flowId: String,
    val scene: CreativeFlowShareScene,
    val shareMode: CreativeFlowShareMode,
    val talentCode: String? = null,
    val sourceProjectId: String,
    val sourcePipelineId: String,
    val sourceVersion: Int,
    val sourceVersionNum: Int? = null,
    val targetProjectId: String,
    val targetPipelineId: String,
    val targetPipelineName: String,
    val targetVersion: Int,
    val targetVersionNum: Int? = null,
    val targetEnvHashId: String? = null,
    val copyAction: CreativeFlowCopyStatus,
    val variableOverrides: String? = null,
    val operator: String,
    val createTime: Long? = null
)
