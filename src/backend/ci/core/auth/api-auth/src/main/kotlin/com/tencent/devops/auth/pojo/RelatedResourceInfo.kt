package com.tencent.devops.auth.pojo

import com.tencent.bk.sdk.iam.dto.InstancePathDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "组权限详情")
data class RelatedResourceInfo(
    @get:Schema(title = "资源类型")
    val type: String,
    @get:Schema(title = "资源类型名")
    val name: String,
    @get:Schema(title = "资源实例")
    val instance: List<List<InstancePathDTO>>
)
