package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("PreProject请求")
data class PreProjectReq(
    @ApiModelProperty("项目名称", required = true)
    val preProjectId: String,
    @ApiModelProperty("工作空间", required = true)
    val workspace: String
)