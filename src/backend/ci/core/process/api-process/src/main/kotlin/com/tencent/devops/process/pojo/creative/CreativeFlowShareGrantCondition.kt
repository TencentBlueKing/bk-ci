package com.tencent.devops.process.pojo.creative

data class CreativeFlowShareGrantCondition(
    val shareId: String? = null,
    val flowId: String? = null,
    val talentCode: String? = null,
    val sourceProjectId: String? = null,
    val sourcePipelineId: String? = null,
    val includeRevoked: Boolean = false
)
