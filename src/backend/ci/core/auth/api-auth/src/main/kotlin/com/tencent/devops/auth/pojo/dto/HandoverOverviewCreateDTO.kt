package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.HandoverStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创建权限交接总览DTO")
data class HandoverOverviewCreateDTO(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "项目名称")
    val projectName: String,
    @get:Schema(title = "项目ID")
    var title: String? = null,
    @get:Schema(title = "流程单号")
    var flowNo: String? = null,
    @get:Schema(title = "申请人")
    val applicant: String,
    @get:Schema(title = "审批人")
    val approver: String,
    @get:Schema(title = "审批结果")
    val handoverStatus: HandoverStatus,
    @get:Schema(title = "用户组个数")
    val groupCount: Int,
    @get:Schema(title = "授权个数")
    val authorizationCount: Int,
    @get:Schema(title = "备注")
    val remark: String? = null
)
