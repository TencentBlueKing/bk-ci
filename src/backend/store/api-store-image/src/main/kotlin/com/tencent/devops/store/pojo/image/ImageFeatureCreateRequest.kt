package com.tencent.devops.store.pojo.image

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像市场-镜像特性信息新增请求报文体")
data class ImageFeatureCreateRequest(
    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String
)