package com.tencent.devops.auth.pojo

import com.tencent.bk.sdk.iam.dto.InstancesDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "组权限详情")
data class RelatedResourceInfo(
    @Schema(description = "资源类型")
    val type: String,
    @Schema(description = "资源类型名")
    val name: String,
    @Schema(description = "资源实例")
    val instances: InstancesDTO
)
