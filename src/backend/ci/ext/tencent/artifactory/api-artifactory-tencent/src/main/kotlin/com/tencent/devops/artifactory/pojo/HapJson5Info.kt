package com.tencent.devops.artifactory.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "hap json5需要的信息")
data class HapJson5Info(
    val userId: String,
    val ttl: Int,
    val experienceHashId: String,
    val filePath: String,
    val organization: String?
)