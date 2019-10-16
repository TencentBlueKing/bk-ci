package com.tencent.devops.common.archive.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.archive.api.pojo.JFrogFileDetail
import com.tencent.devops.common.archive.api.pojo.JFrogFileInfo
import com.tencent.devops.common.archive.api.pojo.JFrogFileInfoList
import com.tencent.devops.common.archive.api.pojo.JFrogProperties
import com.tencent.devops.common.archive.util.JFrogUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream
import javax.ws.rs.NotFoundException

@Component
class JFrogStorageApi @Autowired constructor(
    jFrogConfigProperties: JFrogConfigProperties,
    private val objectMapper: ObjectMapper
) {
//    private val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()
    private val baseUrl = jFrogConfigProperties.url!!
    private val credential = JFrogUtil.makeCredential(jFrogConfigProperties.username!!, jFrogConfigProperties.password!!)

    fun list(path: String, deep: Boolean, depth: Int): List<JFrogFileInfo> {
        val isDeep = if (deep) 1 else 0
        val url = "$baseUrl/api/storage/$path?list&deep=$isDeep&depth=$depth&listFolders=1&mdTimestamps=1&includeRootPath=0"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    logger.info("JFrog $path not found")
                    return emptyList()
                }
                logger.error("Fail to list $path. $responseContent")
                throw RuntimeException("Fail to list artifact")
            }

            val jFrogFileList = objectMapper.readValue<JFrogFileInfoList>(responseContent)
            return jFrogFileList.files
        }
    }

    fun exist(path: String): Boolean {
        try {
            file(path)
            return true
        } catch (e: NotFoundException) {
        }
        return false
    }

    fun file(path: String): JFrogFileDetail {
        val url = "$baseUrl/api/storage/$path"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get jfrog $path. $responseContent")
                if (response.code() == 404) {
                    throw NotFoundException("File not found")
                }
                throw RuntimeException("Fail to get artifact")
            }

            return objectMapper.readValue(responseContent)
        }
    }

    fun deploy(path: String, inputStream: InputStream, properties: Map<String, String>? = null) {
        val sb = StringBuilder()
        sb.append("$baseUrl/$path")
        properties?.forEach { key, value ->
            sb.append(";$key=$value")
        }
        val url = sb.toString()
        val mediaType = MediaType.parse("application/octet-stream")
        val requestBody = object : RequestBody() {
            override fun writeTo(sink: BufferedSink?) {
                val source = Okio.source(inputStream)
                sink!!.writeAll(source)
            }

            override fun contentType(): MediaType? {
                return mediaType
            }
        }

        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .put(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to deploy $path. $responseContent")
                throw RuntimeException("Fail to deploy artifact")
            }
        }
    }

    fun copy(fromPath: String, toPath: String) {
        val url = "$baseUrl/api/copy/$fromPath?to=$toPath"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, "")
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .post(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to copy jfrog from $fromPath to $toPath. $responseContent")
                throw RuntimeException("Fail to copy artifact")
            }
        }
    }

    fun move(fromPath: String, toPath: String) {
        val url = "$baseUrl/api/move/$fromPath?to=$toPath"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, "")
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .post(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to move jfrog from $fromPath to $toPath. $responseContent")
                throw RuntimeException("Fail to move artifact")
            }
        }
    }

    fun delete(path: String) {
        val url = "$baseUrl/$path"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .delete()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to delete jfrog $path. $responseContent")
                throw RuntimeException("Fail to delete artifact")
            }
        }
    }

    fun mkdir(path: String, userId: String? = null) {
        val url = if (userId == null) "$baseUrl/$path" else "$baseUrl/$path;userId=$userId"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, "")
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .put(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to make jfrog directory $path. $responseContent")
                throw RuntimeException("Fail to mkdir")
            }
        }
    }

    fun properties(path: String): Map<String, List<String>> {
        val url = "$baseUrl/api/storage/$path?properties"
        val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .get()
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                if (response.code() == 404) {
                    return mutableMapOf()
                }
                logger.error("Fail to get jfrog $path. $responseContent")
                throw RuntimeException("Fail to get artifact properties")
            }

            val jFrogProperties = objectMapper.readValue<JFrogProperties>(responseContent)
            return jFrogProperties.properties
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JFrogStorageApi::class.java)
    }
}