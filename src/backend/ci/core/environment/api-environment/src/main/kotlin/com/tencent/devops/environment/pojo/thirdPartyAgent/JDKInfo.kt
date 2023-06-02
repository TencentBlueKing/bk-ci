package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("JDK版本信息")
data class JDKInfo(
    @ApiModelProperty("操作系统类型，MACOS/LINUX/WINDOWS")
    val os: String,
    @ApiModelProperty("CPU架构，AMD64/ARM64/MIPS64")
    val archType: String,
    @ApiModelProperty("java -version 版本信息最后一行")
    val jdkVersionString: String
)
