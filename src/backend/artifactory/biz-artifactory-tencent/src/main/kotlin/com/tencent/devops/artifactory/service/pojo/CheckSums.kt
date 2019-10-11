package com.tencent.devops.artifactory.service.pojo

data class CheckSums(
    val sha256: String?,
    val sha1: String,
    val md5: String
)