package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.annotations.ApiModelProperty

data class JobCloudVariableServer(
    @ApiModelProperty(value = "引用的全局变量名称")
    val variable: String,
    @ApiModelProperty(value = "机器信息")
    val jobCloudServer: JobCloudServer
)