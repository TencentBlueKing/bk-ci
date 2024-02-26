package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "操作实体")
data class ActionInfoVo(
    @get:Schema(title = "action")
    val action: String,
    @get:Schema(title = "动作名")
    @BkFieldI18n
    val actionName: String,
    @get:Schema(title = "蓝盾-关联资源类型")
    val resourceType: String,
    @get:Schema(title = "IAM-关联资源类型")
    val relatedResourceType: String
)
