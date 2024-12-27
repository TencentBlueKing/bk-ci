package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.HandoverAction
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "更新权限交接总览请求体")
data class HandoverOverviewUpdateReq(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "流程单号")
    val flowNo: String,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "审批操作")
    val handoverAction: HandoverAction,
    @get:Schema(title = "备注")
    val remark: String? = ""
)
