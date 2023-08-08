package com.tencent.devops.dispatch.devcloud.pojo

data class DestroyContainerReq(
    val projectId: String,
    val pipelineId: String,
    val vmSeqId: Int,
    val containerName: String? = null
)
