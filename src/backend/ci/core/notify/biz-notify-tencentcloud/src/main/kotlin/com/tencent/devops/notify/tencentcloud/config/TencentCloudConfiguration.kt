package com.tencent.devops.notify.tencentcloud.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class TencentCloudConfiguration {

    @Value("\${notify.tencentCloud.secretId:}")
    val secretId: String = ""

    @Value("\${notify.tencentCloud.secretKey:}")
    val secretKey: String = ""

    @Value("\${notify.tencentCloud.emailRegion:ap-hongkong}")
    val emailRegion: String = "ap-hongkong"

    @Value("\${notify.tencentCloud.emailSender:tencent-ci@mail.ci.tencent.com}")
    val emailSender: String = "tencent-ci@mail.ci.tencent.com"
}
