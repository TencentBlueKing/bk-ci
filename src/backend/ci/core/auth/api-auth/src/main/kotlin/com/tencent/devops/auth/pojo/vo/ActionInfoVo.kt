package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "操作实体")
data class ActionInfoVo(
    @Schema(name = "action")
    val action: String,
    @Schema(name = "动作名")
    @BkFieldI18n
    val actionName: String,
    @Schema(name = "蓝盾-关联资源类型")
    val resourceType: String,
    @Schema(name = "IAM-关联资源类型")
    val relatedResourceType: String
)
