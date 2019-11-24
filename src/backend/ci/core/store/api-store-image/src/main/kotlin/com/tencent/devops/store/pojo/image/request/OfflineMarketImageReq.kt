package com.tencent.devops.store.pojo.image.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("下架镜像")
data class OfflineMarketImageReq(
    @ApiModelProperty("镜像要下架的版本号", required = true)
    val version: String,
    @ApiModelProperty("镜像下架原因", required = true)
    val reason: String
)