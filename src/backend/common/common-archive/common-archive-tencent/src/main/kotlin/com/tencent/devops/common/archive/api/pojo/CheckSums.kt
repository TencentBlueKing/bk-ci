package com.tencent.devops.common.archive.api.pojo

data class CheckSums(
    val sha256: String?,
    val sha1: String,
    val md5: String
)