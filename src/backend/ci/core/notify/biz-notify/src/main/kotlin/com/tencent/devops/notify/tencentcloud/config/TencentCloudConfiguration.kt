package com.tencent.devops.notify.tencentcloud.config

data class TencentCloudConfiguration(
    val secretId: String,
    val secretKey: String,
    val emailRegion: String,
    val emailSender: String
)
