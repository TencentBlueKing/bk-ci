package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.annotations.ApiModelProperty

data class JobCloudAccount(
    @ApiModelProperty(value = "账号ID")
    val id: Long,
    @ApiModelProperty(value = "账号名称")
    val name: String?
)