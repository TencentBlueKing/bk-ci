package com.tencent.devops.gitci.pojo.v2.message

data class RequestMessageContent(
    val id: Long,
    val pipelineName: String?,
    val buildBum: Int?,
    val triggerReasonName: String?,
    val triggerReasonDetail: String?,
    val filePathUrl: String?
)
