package com.tencent.devops.experience.pojo.download

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-检查更新参数")
data class CheckVersionParam(
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long
)
