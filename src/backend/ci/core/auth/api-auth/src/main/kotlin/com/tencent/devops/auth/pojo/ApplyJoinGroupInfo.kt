package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "申请加入用户组实体")
data class ApplyJoinGroupInfo(
    @Schema(title = "项目Code")
    val projectCode: String,
    @Schema(title = "用户组id")
    val groupIds: List<Int>,
    @Schema(title = "过期时间")
    val expiredAt: String,
    @Schema(title = "申请人")
    val applicant: String,
    @Schema(title = "申请理由")
    val reason: String
)
