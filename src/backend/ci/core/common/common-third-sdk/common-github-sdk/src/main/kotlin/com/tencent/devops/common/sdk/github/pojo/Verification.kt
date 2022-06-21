package com.tencent.devops.common.sdk.github.pojo

data class Verification(
    val verified: Boolean,
    val resion: String,
    val signature: String?,
    val payload: String?,
)
