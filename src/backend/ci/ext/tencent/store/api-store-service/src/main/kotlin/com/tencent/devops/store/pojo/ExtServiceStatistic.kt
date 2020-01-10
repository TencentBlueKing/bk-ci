package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceStatistic (
    @ApiModelProperty("下载量")
    val downloads: Int,
    @ApiModelProperty("评论量")
    val commentCnt: Int,
    @ApiModelProperty("星级评分")
    val score: Double?,
    @ApiModelProperty("扩展服务code")
    val serviceCode: String
)