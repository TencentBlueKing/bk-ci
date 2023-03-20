package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_THIRDPARTY_SYSTEM_FAIL
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Suppress("NestedBlockDepth", "MaxLineLength")
object OkhttpUtils {

    private val logger = LoggerFactory.getLogger(OkhttpUtils::class.java)

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(3L, TimeUnit.SECONDS)
        .readTimeout(3L, TimeUnit.SECONDS)
        .writeTimeout(3L, TimeUnit.SECONDS)
        .build()

    fun doGet(url: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, url:$url message: ${response.message}")
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "launch third party system fail!")
            }
            return responseContent
        }
    }

    fun doGet(url: String, parameters: Map<String, Any>, headers: Map<String, String>): String {
        var parameterUrl = StringBuffer()
        if (!parameters.isNullOrEmpty()) {
            parameters.forEach { parameterUrl.append("&${it.key}=${it.value}") }
        }
        return doGet(url + parameterUrl, headers)
    }

    fun doHttp(request: Request): Response {
        return okHttpClient.newCall(request).execute()
    }

    fun doHttpPost(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
                )
            )
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, url: $url requestBody: $body message: ${response.message}, content: $responseContent")
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "launch third party system fail!")
            }
            return responseContent
        }
    }

    fun doHttpDelete(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .delete(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
                )
            )
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, message: ${response.message}")
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "launch third party system fail!")
            }
            return responseContent
        }
    }

    fun doHttpPut(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .put(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
                )
            )
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, message: ${response.message}")
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "launch third party system fail!")
            }
            return responseContent
        }
    }

    fun downloadFile(url: String, destPath: File) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                logger.warn("The file $url is not exist")
                throw RuntimeException("文件不存在")
            }
            if (!response.isSuccessful) {
                logger.warn("fail to download the file from $url because of ${response.message} and code ${response.code}")
                throw RuntimeException("获取文件失败")
            }
            if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
            val buf = ByteArray(4096)
            response.body!!.byteStream().use { bs ->
                var len = bs.read(buf)
                FileOutputStream(destPath).use { fos ->
                    while (len != -1) {
                        fos.write(buf, 0, len)
                        len = bs.read(buf)
                    }
                }
            }
        }
    }

    fun downloadFile(response: Response, destPath: File) {
        if (response.code == HttpStatus.NOT_MODIFIED.value()) {
            logger.info("file is newest, do not download to $destPath")
            return
        }
        if (!response.isSuccessful) {
            logger.warn("fail to download the file because of ${response.message} and code ${response.code}")
            throw RuntimeException("获取文件失败")
        }
        if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
        val buf = ByteArray(4096)
        response.body!!.byteStream().use { bs ->
            var len = bs.read(buf)
            FileOutputStream(destPath).use { fos ->
                while (len != -1) {
                    fos.write(buf, 0, len)
                    len = bs.read(buf)
                }
            }
        }
    }
}
