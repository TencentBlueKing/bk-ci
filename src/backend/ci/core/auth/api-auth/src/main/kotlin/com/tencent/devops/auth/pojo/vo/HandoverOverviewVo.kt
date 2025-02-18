package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.enum.HandoverStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "权限交接总览返回体")
data class HandoverOverviewVo(
    @get:Schema(title = "ID")
    val id: Long,
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "项目名称")
    val projectName: String,
    @get:Schema(title = "标题")
    val title: String,
    @get:Schema(title = "流程单号")
    val flowNo: String,
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
    @get:Schema(title = "创建时间")
    val createTime: LocalDateTime,
    @get:Schema(title = "最后修改人")
    val lastOperator: String? = null,
    @get:Schema(title = "是否可以撤销，提单为当前用户并且单据处于审批中")
    val canRevoke: Boolean? = null,
    @get:Schema(title = "是否可以审批，审批人为当前用户并且单据处于审批中")
    val canApproval: Boolean? = null,
    @get:Schema(title = "备注")
    val remark: String? = null
)
