package com.tencent.devops.dockerhost.services.image

open class HandlerContext(
    open val projectId: String,
    open val pipelineId: String,
    open val buildId: String,
    open val vmSeqId: String,
    open val userName: String
)
