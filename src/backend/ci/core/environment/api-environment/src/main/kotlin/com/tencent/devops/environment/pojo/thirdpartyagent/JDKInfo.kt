package com.tencent.devops.environment.pojo.thirdpartyagent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "JDK版本信息")
data class JDKInfo(
    @get:Schema(title = "操作系统类型，MACOS/LINUX/WINDOWS")
    val os: String,
    @get:Schema(title = "CPU架构，AMD64/ARM64/MIPS64")
    val archType: String,
    @get:Schema(title = "java -version 版本信息最后一行")
    val jdkVersionString: String
)
