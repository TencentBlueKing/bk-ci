package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.CollationType
import com.tencent.devops.auth.pojo.enum.HandoverStatus
import com.tencent.devops.auth.pojo.enum.SortType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限交接总览查询")
data class HandoverOverviewQueryReq(
    @get:Schema(title = "成员ID")
    val memberId: String,
    @get:Schema(title = "项目ID")
    val projectCode: String? = null,
    @get:Schema(title = "项目名称")
    val projectName: String? = null,
    @get:Schema(title = "项目ID")
    val title: String? = null,
    @get:Schema(title = "流程单号")
    val flowNo: String? = null,
    @get:Schema(title = "流程单号列表")
    val flowNos: List<String>? = null,
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
    @get:Schema(title = "排序类型")
    val sortType: SortType? = null,
    @get:Schema(title = "排序类型")
    val collationType: CollationType? = null,
    @get:Schema(title = "页数")
    val page: Int? = null,
    @get:Schema(title = "页大小")
    val pageSize: Int? = null
)
