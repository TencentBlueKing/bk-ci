package com.tencent.devops.image.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像列表返回报文")
data class ImageListResp(
    @ApiModelProperty("数据集合", required = false)
    val imageList: List<ImageItem>
)