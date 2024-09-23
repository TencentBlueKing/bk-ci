package com.tencent.devops.remotedev.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BkRepoConfig {
    @Value("\${bkrepo.devx.url:}")
    val bkrepoDevxUrl: String = ""

    @Value("\${bkrepo.devx.headerUserAuth:}")
    val bkrepoDevxHeaderUserAuth: String = ""

    @Value("\${bkrepo.csig.url:}")
    val bkrepoCsigUrl: String = ""

    @Value("\${bkrepo.csig.headerUserAuth:}")
    val bkrepoCsigHeaderUserAuth: String = ""

    fun getRegionConfig(region: BkRepoRegion): BkRepoRegionConfig {
        return when (region) {
            BkRepoRegion.DEVX -> BkRepoRegionConfig(bkrepoDevxUrl, bkrepoDevxHeaderUserAuth)
            BkRepoRegion.CSIG -> BkRepoRegionConfig(bkrepoCsigUrl, bkrepoCsigHeaderUserAuth)
        }
    }
}

enum class BkRepoRegion {
    DEVX,
    CSIG;
}

data class BkRepoRegionConfig(
    val url: String,
    val headerUserAuth: String
)
