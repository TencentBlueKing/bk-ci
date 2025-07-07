package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.HandoverType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限交接详细表")
data class HandoverDetailDTO(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "流程单号")
    var flowNo: String? = null,
    @get:Schema(title = "授权/组ID")
    val itemId: String,
    @get:Schema(title = "组/授权资源关联的资源类型")
    val resourceType: String,
    @get:Schema(title = "交接类型")
    val handoverType: HandoverType,
    @get:Schema(title = "审批人")
    var approver: String? = null
)
