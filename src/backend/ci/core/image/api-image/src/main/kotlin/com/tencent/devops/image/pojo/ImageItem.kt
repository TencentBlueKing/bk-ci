package com.tencent.devops.image.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像列表")
data class ImageItem(
    @ApiModelProperty("镜像仓库地址", required = true)
    var repoUrl: String,
    @ApiModelProperty("镜像在仓库中的位置", required = true)
    var repo: String,
    @ApiModelProperty("镜像名称", required = true)
    var name: String
)
