package com.tencent.devops.wetest.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTestFunctionTaskResponse")
data class WeTestFunctionTaskResponse(
    @ApiModelProperty("配置详情")
    val records: List<WeTestFunctionalTask>,
    @ApiModelProperty("状态")
    val count: Int
)
