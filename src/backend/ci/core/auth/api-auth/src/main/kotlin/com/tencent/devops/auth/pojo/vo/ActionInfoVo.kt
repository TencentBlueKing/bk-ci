package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "操作实体")
data class ActionInfoVo(
    @Schema(description = "action")
    val action: String,
    @Schema(description = "动作名")
    @BkFieldI18n
    val actionName: String,
    @Schema(description = "蓝盾-关联资源类型")
    val resourceType: String,
    @Schema(description = "IAM-关联资源类型")
    val relatedResourceType: String
)
