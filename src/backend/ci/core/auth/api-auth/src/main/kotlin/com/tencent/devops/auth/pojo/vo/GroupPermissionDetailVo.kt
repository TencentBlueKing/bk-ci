package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.RelatedResourceInfo
import com.tencent.devops.common.api.annotation.BkFieldI18n
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "组权限详情")
data class GroupPermissionDetailVo(
    @get:Schema(title = "操作id")
    val actionId: String,
    @get:Schema(title = "操作名")
    @BkFieldI18n(convertName = "actionName")
    val name: String,
    @get:Schema(title = "关联资源")
    val relatedResourceInfo: RelatedResourceInfo
)
