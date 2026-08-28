package com.tencent.devops.process.engine.dao.creative

import com.tencent.devops.process.enums.CreativeFlowShareGrantStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareScene
import com.tencent.devops.process.enums.CreativeFlowShareVersionScope

data class CreativeFlowShareGrant(
    val shareId: String,
    val flowId: String,
    val scene: CreativeFlowShareScene,
    val shareMode: CreativeFlowShareMode,
    val sourceProjectId: String,
    val sourcePipelineId: String,
    val versionScope: CreativeFlowShareVersionScope,
    val version: Int? = null,
    val versionNum: Int? = null,
    val validateRulesJson: String? = null,
    val extInfoJson: String? = null,
    val talentCode: String? = null,
    val status: CreativeFlowShareGrantStatus,
    val grantedBy: String,
    val grantedTime: Long,
    val revokedBy: String? = null,
    val revokedTime: Long? = null,
    val updateTime: Long? = null
)
