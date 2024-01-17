package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线数量相关")
data class PipelineCount(
    @Schema(title = "全部流水线个数", required = true)
    var totalCount: Int,
    @Schema(title = "我的收藏个数", required = true)
    var myFavoriteCount: Int,
    @Schema(title = "我的流水线的个数", required = true)
    var myPipelineCount: Int,
    @Schema(title = "回收站流水线的个数", required = true)
    var recycleCount: Int,
    @Schema(title = "最近使用的流水线的个数", required = true)
    val recentUseCount: Int
)
