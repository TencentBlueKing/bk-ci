package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "JDK版本信息")
data class JDKInfo(
    @Schema(name = "操作系统类型，MACOS/LINUX/WINDOWS")
    val os: String,
    @Schema(name = "CPU架构，AMD64/ARM64/MIPS64")
    val archType: String,
    @Schema(name = "java -version 版本信息最后一行")
    val jdkVersionString: String
)
