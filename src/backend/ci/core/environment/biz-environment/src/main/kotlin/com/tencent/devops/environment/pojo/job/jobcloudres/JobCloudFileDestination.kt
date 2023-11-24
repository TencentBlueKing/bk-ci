package com.tencent.devops.environment.pojo.job.jobcloudres

import com.tencent.devops.environment.pojo.job.resp.Account
import io.swagger.annotations.ApiModelProperty

data class JobCloudFileDestination (
    @ApiModelProperty(value = "目标路径")
    val path: String,
    @ApiModelProperty(value = "执行账号")
    val account: Account,
    @ApiModelProperty(value = "分发目标机器")
    val server: JobCloudVariableServer
)