package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线数量相关")
data class PipelineCount(
    @ApiModelProperty("全部流水线个数", required = true)
    var totalCount: Int,
    @ApiModelProperty("我的收藏个数", required = true)
    var myFavoriteCount: Int,
    @ApiModelProperty("我的流水线的个数", required = true)
    var myPipelineCount: Int,
    @ApiModelProperty("回收站流水线的个数", required = true)
    var recycleCount: Int,
    @ApiModelProperty("最近使用的流水线的个数", required = true)
    val recentUseCount: Int
)
