package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class RemoteDevBkRepoConfig {

    @Bean
    @ConfigurationProperties(prefix = "bkrepo.devx")
    fun devxBkRepoRegionConfig(): BkRepoRegionConfig = BkRepoRegionConfig()

    @Bean
    @ConfigurationProperties(prefix = "bkrepo.devxmedia")
    fun devxMediaBkRepoRegionConfig(): BkRepoRegionConfig = BkRepoRegionConfig()

    @Bean
    @ConfigurationProperties(prefix = "bkrepo.csig")
    fun csigBkRepoRegionConfig(): BkRepoRegionConfig = BkRepoRegionConfig()

    @Bean
    @ConfigurationProperties(prefix = "bkrepo.devcloud")
    fun devcloudBkRepoRegionConfig(): BkRepoRegionConfig = BkRepoRegionConfig()

    @Bean
    @ConfigurationProperties(prefix = "bkrepo.devcloudmedia")
    fun devcloudMediaBkRepoRegionConfig(): BkRepoRegionConfig = BkRepoRegionConfig()

    @Qualifier("devxBkRepoRegionConfig")
    @Autowired
    private lateinit var devxConfig: BkRepoRegionConfig

    @Qualifier("devxMediaBkRepoRegionConfig")
    @Autowired
    private lateinit var devxMediaConfig: BkRepoRegionConfig

    @Qualifier("csigBkRepoRegionConfig")
    @Autowired
    private lateinit var csigConfig: BkRepoRegionConfig

    @Qualifier("devcloudBkRepoRegionConfig")
    @Autowired
    private lateinit var devcloudConfig: BkRepoRegionConfig

    @Qualifier("devcloudMediaBkRepoRegionConfig")
    @Autowired
    private lateinit var devcloudMediaConfig: BkRepoRegionConfig

    fun getRegionConfig(region: BkRepoRegion, media: Boolean = false): BkRepoRegionConfig {
        return when (region) {
            BkRepoRegion.DEVX -> {
                if (media) {
                    devxMediaConfig
                }
                devxConfig
            }
            BkRepoRegion.CSIG -> csigConfig
            BkRepoRegion.DEVCLOUD -> {
                if (media) {
                    devcloudMediaConfig
                }
                devcloudConfig
            }
        }
    }
}

enum class BkRepoRegion {
    DEVX,
    CSIG,
    DEVCLOUD
}

data class BkRepoRegionConfig(
    var url: String = "",
    var headerUserAuth: String = "",
    var webUrl: String = "",
    var dnsIp: String = ""
)
