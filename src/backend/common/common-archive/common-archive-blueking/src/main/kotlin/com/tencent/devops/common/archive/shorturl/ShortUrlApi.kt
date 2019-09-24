package com.tencent.devops.common.archive.shorturl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.shorturl.pojo.ShortUrlRequest
import com.tencent.devops.common.archive.shorturl.pojo.ShortUrlResponse
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ShortUrlApi @Autowired constructor(private val objectMapper: ObjectMapper) {

    fun getShortUrl(url: String, ttl: Int): String {
        val timestamp = LocalDateTime.now().plusSeconds(ttl.toLong()).timestamp()
        val shortUrlRequest = ShortUrlRequest(1, APPID, PASSWORD, url, 1, timestamp)
        val requestContent = objectMapper.writeValueAsString(shortUrlRequest)

        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, requestContent)
        val request = Request.Builder()
                .url(URL)
                .post(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get short url. $responseContent")
                throw RuntimeException("Fail to get short url")
            }

            val shortUrlResponse = objectMapper.readValue<ShortUrlResponse>(responseContent)
            return shortUrlResponse.shortUrl
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ShortUrlApi::class.java)
        private const val URL = "http://makeshorturl.wsd.com"
        private const val APPID = 71
        private const val PASSWORD = "3IEZwzKABD6vZEWw"
    }
}