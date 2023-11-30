package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudAccountAlias(
    @ApiModelProperty(value = "执行帐号ID")
    val id: Long?,
    @ApiModelProperty(value = "执行帐号别名")
    val alias: String?
)