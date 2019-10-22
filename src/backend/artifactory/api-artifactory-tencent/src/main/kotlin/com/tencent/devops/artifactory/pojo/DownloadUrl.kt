package com.tencent.devops.artifactory.pojo

import com.tencent.devops.artifactory.pojo.enums.Platform
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-下载信息")
data class DownloadUrl(
    @ApiModelProperty("下载链接", required = true)
    val url: String,
    @ApiModelProperty("平台", required = true)
    val platform: Platform
)