package com.tencent.devops.dockerhost.pojo

@SuppressWarnings("ALL")
data class LeakScanReq(
    val content: String,
    val format_type: String
)
