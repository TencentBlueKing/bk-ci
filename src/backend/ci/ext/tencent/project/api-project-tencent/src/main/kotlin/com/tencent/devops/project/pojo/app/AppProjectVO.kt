package com.tencent.devops.project.pojo.app

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-APP")
data class AppProjectVO(
    @ApiModelProperty("项目代码")
    val projectCode: String,
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("logo地址")
    val logoUrl: String?
)