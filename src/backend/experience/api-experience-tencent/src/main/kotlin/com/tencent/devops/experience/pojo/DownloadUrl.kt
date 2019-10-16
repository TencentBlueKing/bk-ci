package com.tencent.devops.experience.pojo

import com.tencent.devops.experience.pojo.enums.Platform
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-下载信息")
data class DownloadUrl(
    @ApiModelProperty("下载链接", required = true)
    val url: String,
    @ApiModelProperty("平台", required = true)
    val platform: Platform,
    @ApiModelProperty("大小(byte)", required = true)
    val size: Long
)