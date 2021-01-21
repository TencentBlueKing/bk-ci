package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModelProperty

data class StpProjectInfo(
    @ApiModelProperty("项目Id")
    val projectid: String,
    @ApiModelProperty("名称")
    val projectname: String
)
