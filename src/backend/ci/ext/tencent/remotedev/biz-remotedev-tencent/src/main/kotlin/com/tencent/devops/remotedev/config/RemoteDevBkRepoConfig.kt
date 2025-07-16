package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class RemoteDevBkRepoConfig {
    @Value("\${bkrepo.devx.url:}")
    val bkrepoDevxUrl: String = ""

    @Value("\${bkrepo.devx.headerUserAuth:}")
    val bkrepoDevxHeaderUserAuth: String = ""

    @Value("\${bkrepo.devx.proxy:}")
    val bkrepoDevxProxyUrl: String = ""

    @Value("\${bkrepo.csig.url:}")
    val bkrepoCsigUrl: String = ""

    @Value("\${bkrepo.csig.headerUserAuth:}")
    val bkrepoCsigHeaderUserAuth: String = ""

    @Value("\${bkrepo.csig.webUrl:}")
    val bkrepoCsigWebUrl: String = ""

    fun getRegionConfig(region: BkRepoRegion): BkRepoRegionConfig {
        return when (region) {
            BkRepoRegion.DEVX -> BkRepoRegionConfig(bkrepoDevxUrl, bkrepoDevxHeaderUserAuth, bkrepoDevxUrl, bkrepoDevxProxyUrl)
            BkRepoRegion.CSIG -> BkRepoRegionConfig(bkrepoCsigUrl, bkrepoCsigHeaderUserAuth, bkrepoCsigWebUrl, null)
        }
    }
}

enum class BkRepoRegion {
    DEVX,
    CSIG
}

data class BkRepoRegionConfig(
    private val url: String,
    val headerUserAuth: String,
    val webUrl: String,
    val proxy: String?
) {
    fun genUrl(uri: String): String {
        val url = "$url$uri"
        return if (proxy == null) {
            url
        } else {
            "$proxy?url=${URLEncoder.encode(url, "UTF-8")}"
        }
    }
}
