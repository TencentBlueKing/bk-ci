package com.tencent.devops.common.misc.client

import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.Headers
import okhttp3.Request
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang.RandomStringUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory

object DevCloudContainerInstanceClient {

    private val logger = LoggerFactory.getLogger(DevCloudContainerInstanceClient::class.java)

    fun getContainerInstance(
        devCloudUrl: String,
        devCloudAppId: String,
        devCloudToken: String,
        staffName: String,
        id: String
    ): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$id/instances"
        logger.info("request url: $url")
        val request = Request.Builder()
            .url(url)
            .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getHeaders(appId: String, token: String, staffName: String): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        headerBuilder["STAFFNAME"] = staffName
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey

        return headerBuilder
    }
}