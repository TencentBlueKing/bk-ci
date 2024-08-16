package com.tencent.devops.dispatch.devcloud.pojo

data class DestroyContainerReq(
    val projectId: String,
    val pipelineId: String,
    val vmSeqId: String,
    val poolNo: Int? = null,
    val persistenceAgentId: String? = null
)
