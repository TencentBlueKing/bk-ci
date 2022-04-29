package com.tencent.devops.dispatch.bcs.utils

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils

/**
 * 智能网关工具类
 */
object SmartProxyUtil {

    fun makeHeaders(appId: String, token: String, staffName: String, proxyToken: String): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        headerBuilder["STAFFNAME"] = staffName
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey
        headerBuilder["TIMESTAMP"] = timestamp
        val staffId = "mock"
        headerBuilder["STAFFID"] = staffId
        headerBuilder["X-EXT-DATA"] = ""
        val seq = "mock"
        headerBuilder["X-RIO-SEQ"] = seq
        val signature = ShaUtils.sha256("$timestamp$proxyToken$seq,$staffId,$staffName,$timestamp")
        headerBuilder["SIGNATURE"] = signature.toUpperCase()

        return headerBuilder
    }
}
