package com.tencent.devops.sign.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IPA Info Plist内容信息")
data class IpaInfoPlist(
    @ApiModelProperty("bundleId", required = true)
    var bundleIdentifier: String = "",
    @ApiModelProperty("应用名字", required = true)
    var appTitle: String = "",
    @ApiModelProperty("应用版本", required = true)
    var bundleVersion: String = "",
    @ApiModelProperty("应用构建版本", required = true)
    var bundleVersionFull: String = ""
)