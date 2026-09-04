package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流 Stage 审核提示（build_status 附加字段，前端可忽略）")
data class CreativeStreamStageReviewHint(
    @get:Schema(title = "当前待审 Stage ID")
    val stageId: String,
    @get:Schema(title = "当前待审审核组 ID")
    val groupId: String? = null,
    @get:Schema(title = "当前待审审核组名")
    val groupName: String,
    @get:Schema(title = "是否必须走 imate 会话锁定（仅 IMATE 组且已绑定会话）")
    val imateLockRequired: Boolean,
    @get:Schema(title = "CDS 审批 taskId，仅 imateLockRequired=true 时有值")
    val taskId: String? = null,
    @get:Schema(title = "SaaS App ID")
    val saasId: String? = null
)
