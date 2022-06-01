package com.tencent.bkrepo.git.config

import com.tencent.bkrepo.git.constant.HubType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties("git")
data class GitProperties(
    var storageCredentialsKey: String ? = null,
    var locationDir: String ? = null,
    @NestedConfigurationProperty
    var hub: Hub = Hub()
) {

    data class Hub(var github: String? = null)

    fun getDomain(hubType: HubType): String? {
        return when (hubType) {
            HubType.GITHUB -> hub.github
            else -> null
        }
    }
}
