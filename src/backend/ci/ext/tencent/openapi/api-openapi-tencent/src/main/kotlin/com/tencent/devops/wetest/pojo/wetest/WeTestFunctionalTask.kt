package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModelProperty

data class WeTestFunctionalTask(
    @ApiModelProperty("项目Id")
    val taskid: String,
    @ApiModelProperty("名称")
    val taskname: String
)
