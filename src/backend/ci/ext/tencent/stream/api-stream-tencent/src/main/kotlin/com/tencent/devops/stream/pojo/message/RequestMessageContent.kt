package com.tencent.devops.stream.pojo.message

data class RequestMessageContent(
    val id: Long,
    val pipelineName: String?,
    val buildBum: Int?,
    val triggerReasonName: String?,
    val triggerReasonDetail: String?,
    val filePathUrl: String?
)
