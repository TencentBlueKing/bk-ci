package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("新增修改原因请求")
data class ReasonReq(
    @ApiModelProperty("原因", required = true)
    val content: String,
    @ApiModelProperty("顺序", required = true)
    val order: Int
)