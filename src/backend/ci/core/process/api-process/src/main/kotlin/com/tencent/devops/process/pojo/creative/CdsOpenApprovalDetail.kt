package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "CDS 开放审批详情（只保留闸门需要的字段）")
data class CdsOpenApprovalDetail(
    val taskId: String? = null,
    val saasId: String? = null,
    val sessionKey: String? = null,
    val conversationId: String? = null,
    val status: String? = null,
    val approver: String? = null,
    val approveComment: String? = null
)
