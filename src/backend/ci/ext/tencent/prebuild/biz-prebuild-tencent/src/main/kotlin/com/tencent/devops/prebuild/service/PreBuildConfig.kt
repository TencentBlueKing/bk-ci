package com.tencent.devops.prebuild.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PreBuildConfig {

    @Value("\${codeCC.clientImage:#{null}}")
    val codeCCSofwareClientImage: String? = null

    @Value("\${codeCC.softwarePath:#{null}}")
    val codeCCSofwarePath: String? = null

    @Value("\${registry.host:#{null}}")
    val registryHost: String? = null

    @Value("\${registry.userName:#{null}}")
    val registryUserName: String? = null

    @Value("\${registry.password:#{null}}")
    val registryPassword: String? = null

    @Value("\${registry.image:#{null}}")
    val registryImage: String? = null

    @Value("\${devCloud.cpu:16}")
    val cpu: Int = 16

    @Value("\${devCloud.memory:32767M}")
    val memory: String = "32767M"

    @Value("\${devCloud.disk:50G}")
    val disk: String = "50G"

    @Value("\${devCloud.volume:100}")
    val volume: Int = 100

    @Value("\${devCloud.activeDeadlineSeconds:86400}")
    val activeDeadlineSeconds: Int = 86400

    @Value("\${devCloud.appId:#{null}}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token:#{null}}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url:#{null}}")
    val devCloudUrl: String = ""
}