package com.tencent.devops.store.pojo.image

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像统计信息")
data class ImageStatistic(
    @ApiModelProperty("下载量")
    val downloads: Int,
    @ApiModelProperty("评论量")
    val commentCnt: Int,
    @ApiModelProperty("星级评分")
    val score: Double?
)