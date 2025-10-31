package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RemoteDevBkRepoConfig {
    @Value("\${bkrepo.devx.url:}")
    val bkrepoDevxUrl: String = ""

    @Value("\${bkrepo.devx.headerUserAuth:}")
    val bkrepoDevxHeaderUserAuth: String = ""

    @Value("\${bkrepo.devx.dnsIp:}")
    val bkrepoDevxDnsIp: String = ""

    @Value("\${bkrepo.csig.url:}")
    val bkrepoCsigUrl: String = ""

    @Value("\${bkrepo.csig.headerUserAuth:}")
    val bkrepoCsigHeaderUserAuth: String = ""

    @Value("\${bkrepo.csig.webUrl:}")
    val bkrepoCsigWebUrl: String = ""

    fun getRegionConfig(region: BkRepoRegion): BkRepoRegionConfig {
        return when (region) {
            BkRepoRegion.DEVX -> BkRepoRegionConfig(
                url = bkrepoDevxUrl,
                headerUserAuth = bkrepoDevxHeaderUserAuth,
                webUrl = bkrepoDevxUrl,
                dnsIp = bkrepoDevxDnsIp
            )

            BkRepoRegion.CSIG -> BkRepoRegionConfig(bkrepoCsigUrl, bkrepoCsigHeaderUserAuth, bkrepoCsigWebUrl, null)
        }
    }
}

enum class BkRepoRegion {
    DEVX,
    CSIG
}

data class BkRepoRegionConfig(
    val url: String,
    val headerUserAuth: String,
    val webUrl: String,
    val dnsIp: String?
)
