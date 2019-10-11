package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-下载信息")
data class Url(
    @ApiModelProperty("下载链接", required = true)
    val url: String,
    @ApiModelProperty("下载链接2", required = false)
    val url2: String? = null
)