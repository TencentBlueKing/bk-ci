package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.HandoverAction
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量更新权限交接请求体")
data class HandoverOverviewBatchUpdateReq(
    @get:Schema(title = "流程单号")
    val flowNos: List<String> = emptyList(),
    @get:Schema(title = "是否全选")
    val allSelection: Boolean = false,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "审批操作")
    val handoverAction: HandoverAction,
    @get:Schema(title = "备注")
    val remark: String? = null
)
