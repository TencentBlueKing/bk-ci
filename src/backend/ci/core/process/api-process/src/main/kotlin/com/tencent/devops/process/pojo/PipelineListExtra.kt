package com.tencent.devops.process.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线列表额外字段")
data class PipelineListExtra(
    @ApiModelProperty("我的收藏个数")
    val myFavoriteCount: Int,
    @ApiModelProperty("我创建的个数")
    val myCreatedCount: Int
)
