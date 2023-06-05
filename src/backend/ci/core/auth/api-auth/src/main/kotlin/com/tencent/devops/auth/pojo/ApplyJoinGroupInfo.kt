package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("申请加入用户组实体")
data class ApplyJoinGroupInfo(
    @ApiModelProperty("用户组id")
    val groupIds: List<Int>,
    @ApiModelProperty("过期时间")
    val expiredAt: String,
    @ApiModelProperty("申请人")
    val applicant: String,
    @ApiModelProperty("申请理由")
    val reason: String
)
