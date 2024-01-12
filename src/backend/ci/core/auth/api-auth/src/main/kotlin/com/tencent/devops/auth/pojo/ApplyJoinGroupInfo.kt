package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "申请加入用户组实体")
data class ApplyJoinGroupInfo(
    @Schema(description = "项目Code")
    val projectCode: String,
    @Schema(description = "用户组id")
    val groupIds: List<Int>,
    @Schema(description = "过期时间")
    val expiredAt: String,
    @Schema(description = "申请人")
    val applicant: String,
    @Schema(description = "申请理由")
    val reason: String
)
