package com.tencent.devops.gitci.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BuildConfig {

    @Value("\${codeCC.softwarePath}")
    val codeCCSofwarePath: String? = null

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUserName: String? = null

    @Value("\${registry.password}")
    val registryPassword: String? = null

    @Value("\${registry.image}")
    val registryImage: String? = null

    @Value("\${devCloud.cpu}")
    val cpu: Int = 32

    @Value("\${devCloud.memory}")
    val memory: String = "65535M"

    @Value("\${devCloud.disk}")
    val disk: String = "500G"

    @Value("\${devCloud.volume}")
    val volume: Int = 100

    @Value("\${devCloud.activeDeadlineSeconds}")
    val activeDeadlineSeconds: Int = 86400

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    val devCloudUrl: String = ""
}