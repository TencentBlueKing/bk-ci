package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "JDK版本信息")
data class JDKInfo(
    @Schema(description = "操作系统类型，MACOS/LINUX/WINDOWS")
    val os: String,
    @Schema(description = "CPU架构，AMD64/ARM64/MIPS64")
    val archType: String,
    @Schema(description = "java -version 版本信息最后一行")
    val jdkVersionString: String
)
