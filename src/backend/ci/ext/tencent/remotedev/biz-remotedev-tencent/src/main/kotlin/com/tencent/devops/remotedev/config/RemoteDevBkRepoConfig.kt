package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RemoteDevBkRepoConfig {
    @Value("\${bkrepo.devx.url:}")
    val bkrepoDevxUrl: String = ""

    @Value("\${bkrepo.devx.headerUserAuth:}")
    val bkrepoDevxHeaderUserAuth: String = ""

    @Value("\${bkrepo.csig.url:}")
    val bkrepoCsigUrl: String = ""

    @Value("\${bkrepo.csig.headerUserAuth:}")
    val bkrepoCsigHeaderUserAuth: String = ""

    @Value("\${bkrepo.csig.webUrl:}")
    val bkrepoCsigWebUrl: String = ""

    fun getRegionConfig(region: BkRepoRegion): BkRepoRegionConfig {
        return when (region) {
            BkRepoRegion.DEVX -> BkRepoRegionConfig(bkrepoDevxUrl, bkrepoDevxHeaderUserAuth, bkrepoDevxUrl)
            BkRepoRegion.CSIG -> BkRepoRegionConfig(bkrepoCsigUrl, bkrepoCsigHeaderUserAuth, bkrepoCsigWebUrl)
        }
    }
}

enum class BkRepoRegion {
    DEVX,
    CSIG;
}

data class BkRepoRegionConfig(
    val url: String,
    val headerUserAuth: String,
    val webUrl: String
)
