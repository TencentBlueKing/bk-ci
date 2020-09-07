package com.tencent.devops.lambda.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建详情")
data class LambdaBuildDetail(
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("构建ID", required = false)
    val buildNum: Int?,
    @ApiModelProperty("构建ID", required = true)
    val model: String,
    @ApiModelProperty("构建ID", required = false)
    val startUser: String?,
    @ApiModelProperty("构建ID", required = false)
    val trigger: String?,
    @ApiModelProperty("构建ID", required = false)
    val startTime: Long,
    @ApiModelProperty("构建ID", required = false)
    val endTime: Long?,
    @ApiModelProperty("构建ID", required = false)
    val status: String?
)