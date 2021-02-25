package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("StpProjectListResponse")
data class StpProjectListResponse(
    @ApiModelProperty("项目列表")
    val records: List<StpProjectInfo>,
    @ApiModelProperty("个数")
    val count: Int
)
