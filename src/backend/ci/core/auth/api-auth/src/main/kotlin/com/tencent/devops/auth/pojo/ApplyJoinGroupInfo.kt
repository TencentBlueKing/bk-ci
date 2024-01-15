package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "申请加入用户组实体")
data class ApplyJoinGroupInfo(
    @Schema(name = "项目Code")
    val projectCode: String,
    @Schema(name = "用户组id")
    val groupIds: List<Int>,
    @Schema(name = "过期时间")
    val expiredAt: String,
    @Schema(name = "申请人")
    val applicant: String,
    @Schema(name = "申请理由")
    val reason: String
)
