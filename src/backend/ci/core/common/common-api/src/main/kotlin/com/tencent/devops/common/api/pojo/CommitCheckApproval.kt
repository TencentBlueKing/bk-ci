package com.tencent.devops.common.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Commit Check 审批项")
data class CommitCheckApproval(
    @get:Schema(title = "审批链接")
    val approveUrl: String? = null,
    @get:Schema(title = "审批人员列表，逗号分隔")
    val approveUsers: String? = null,
    @get:Schema(title = "是否支持快速审批，0:不支持 1:支持")
    val quickApproveEnabled: Int? = null
)
