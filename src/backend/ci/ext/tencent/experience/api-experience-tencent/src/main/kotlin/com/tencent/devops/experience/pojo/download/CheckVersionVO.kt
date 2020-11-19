package com.tencent.devops.experience.pojo.download

import io.swagger.annotations.ApiModelProperty

data class CheckVersionVO(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String
)
