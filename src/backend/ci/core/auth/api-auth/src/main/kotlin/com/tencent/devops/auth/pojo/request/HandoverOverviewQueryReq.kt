package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.HandoverStatus
import com.tencent.devops.auth.pojo.enum.HandoverType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限交接总览查询")
data class HandoverOverviewQueryReq(
    @get:Schema(title = "成员ID")
    val memberID: String,
    @get:Schema(title = "项目ID")
    val projectCode: String? = null,
    @get:Schema(title = "项目ID")
    val title: String? = null,
    @get:Schema(title = "流程单号")
    val flowNo: String? = null,
    @get:Schema(title = "申请人")
    val applicant: String? = null,
    @get:Schema(title = "审批人")
    val approver: String? = null,
    @get:Schema(title = "审批结果")
    val handoverStatus: HandoverStatus? = null,
    @get:Schema(title = "最小提单时间")
    val minCreatedTime: Long? = null,
    @get:Schema(title = "最打提单时间")
    val maxCreatedTime: Long? = null,
    @get:Schema(title = "交接类型")
    val handoverType: HandoverType? = null,
    @get:Schema(title = "限制")
    val limit: Int? = null,
    @get:Schema(title = "起始值")
    val offset: Int? = null
)
