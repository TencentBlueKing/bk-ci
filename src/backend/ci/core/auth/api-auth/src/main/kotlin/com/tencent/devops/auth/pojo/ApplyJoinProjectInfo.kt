package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-申请加入项目实体类")
data class ApplyJoinProjectInfo(
    @ApiModelProperty("过期时间")
    val expireTime: String,
    @ApiModelProperty("申请理由")
    val reason: String
)
