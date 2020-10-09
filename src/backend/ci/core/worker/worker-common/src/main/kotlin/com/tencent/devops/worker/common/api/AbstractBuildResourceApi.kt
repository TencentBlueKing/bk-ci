/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.worker.common.api.utils.ThirdPartyAgentBuildInfoUtils
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.logger.LoggerService
import io.undertow.util.StatusCodes
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.file.Paths
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

abstract class AbstractBuildResourceApi : WorkerRestApiSDK {

    protected fun requestForResponse(
        request: Request,
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null,
        retryCount: Int = DEFAULT_RETRY_TIME
    ): Response {
        val builder = okHttpClient.newBuilder()
        if (connectTimeoutInSec != null) {
            builder.connectTimeout(connectTimeoutInSec, TimeUnit.SECONDS)
        }
        if (readTimeoutInSec != null) {
            builder.readTimeout(readTimeoutInSec, TimeUnit.SECONDS)
        }
        if (writeTimeoutInSec != null) {
            builder.writeTimeout(writeTimeoutInSec, TimeUnit.SECONDS)
        }
        val httpClient = builder.build()
        val retryFlag = try {
            return httpClient.newCall(request).execute()
        } catch (e: UnknownHostException) { // #2353 DNS问题导致请求未到达目标，可重试
            logger.warn("UnknownHostException|request($request),error is :$e, try to retry $retryCount")
            true
        } catch (e: Exception) {
            if (e is SocketTimeoutException && e.message == "timeout") { // #2353 请求没到达服务器而超时，可重试
                logger.warn("SocketTimeoutException|request($request),error is :$e, try to retry $retryCount")
                true
            } else {
                logger.error("Fail to request($request),error is :$e", e)
                throw ClientException("Fail to request($request),error is:${e.message}")
            }
        }

        if (retryFlag && retryCount > 0) {
            logger.warn(
                "Fail to request($request), retry after $sleepTimeMills ms"
            )
            Thread.sleep(sleepTimeMills)
            return requestForResponse(request, connectTimeoutInSec, readTimeoutInSec, writeTimeoutInSec, retryCount - 1)
        } else {
            logger.error("Fail to request($request), try to retry $DEFAULT_RETRY_TIME")
            throw ClientException("Fail to request($request), try to retry $DEFAULT_RETRY_TIME")
        }
    }

    protected fun request(
        request: Request,
        errorMessage: String,
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ): String {

        requestForResponse(
            request = request,
            connectTimeoutInSec = connectTimeoutInSec,
            readTimeoutInSec = readTimeoutInSec,
            writeTimeoutInSec = writeTimeoutInSec
        ).use { response ->
            if (!response.isSuccessful) {
                val responseContent = response.body()?.string()
                logger.warn(
                    "Fail to request($request) with code ${response.code()} ," +
                        " message ${response.message()} and response ($responseContent)"
                )
                throw RemoteServiceException(errorMessage, response.code(), responseContent)
            }
            return response.body()!!.string()
        }
    }

    protected fun download(request: Request, destPath: File) {
        okHttpClient.newBuilder().build().newCall(request).execute().use { response ->
            download(response, destPath)
        }
    }

    private fun download(response: Response, destPath: File) {
        if (response.code() == StatusCodes.NOT_FOUND) {
            throw RemoteServiceException("文件不存在")
        }
        if (!response.isSuccessful) {
            LoggerService.addNormalLine(response.body()!!.string())
            throw RemoteServiceException("获取文件失败")
        }
        if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
        LoggerService.addNormalLine("save file >>>> ${destPath.canonicalPath}")

        response.body()!!.byteStream().use { bs ->
            val buf = ByteArray(BYTE_ARRAY_SIZE)
            var len = bs.read(buf)
            FileOutputStream(destPath).use { fos ->
                while (len != -1) {
                    fos.write(buf, 0, len)
                    len = bs.read(buf)
                }
            }
        }
    }

    companion object {
        val JsonMediaType = MediaType.parse("application/json; charset=utf-8")
        val OctetMediaType = MediaType.parse("application/octet-stream")
        val MultipartFormData = MediaType.parse("multipart/form-data")
        private const val EMPTY = ""
        private const val DEFAULT_RETRY_TIME = 5
        private const val sleepTimeMills = 5000L
        private const val BYTE_ARRAY_SIZE = 4096
        private const val CONNECT_TIMEOUT = 5L
        private const val READ_TIMEOUT = 1500L
        private const val WRITE_TIMEOUT = 60L
        private val retryCodes = arrayOf(502, 503, 504)
        val logger = LoggerFactory.getLogger(AbstractBuildResourceApi::class.java)
        private val gateway = AgentEnv.getGateway()

        private val buildArgs: Map<String, String> by lazy {
            initBuildArgs()
        }

        private fun initBuildArgs(): Map<String, String> {
            val buildType = BuildEnv.getBuildType()
            val map = mutableMapOf<String, String>()

            map[AUTH_HEADER_DEVOPS_BUILD_TYPE] = buildType.name
            when (buildType) {
                BuildType.AGENT -> {
                    map[AUTH_HEADER_DEVOPS_PROJECT_ID] = AgentEnv.getProjectId()
                    map[AUTH_HEADER_DEVOPS_AGENT_ID] = AgentEnv.getAgentId()
                    map[AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY] = AgentEnv.getAgentSecretKey()
                    map[AUTH_HEADER_DEVOPS_PROJECT_ID] = AgentEnv.getProjectId()
                    map[AUTH_HEADER_DEVOPS_AGENT_ID] = AgentEnv.getAgentId()
//                    map[AUTH_HEADER_AGENT_SECRET_KEY] = AgentEnv.getAgentSecretKey()
                }
                BuildType.DOCKER -> {
                    map[AUTH_HEADER_DEVOPS_PROJECT_ID] = AgentEnv.getProjectId()
                    map[AUTH_HEADER_DEVOPS_AGENT_ID] = AgentEnv.getAgentId()
                    map[AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY] = AgentEnv.getAgentSecretKey()
                }
            }
            logger.info("Get the request header - $map")
            return map
        }
    }

    private fun sslSocketFactory(): SSLSocketFactory {
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (ingored: Exception) {
            throw RemoteServiceException(ingored.message!!)
        }
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS) // Set to 15 minutes
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    protected val objectMapper = JsonUtil.getObjectMapper()

    fun buildGet(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).get().build()
    }

    fun buildPost(path: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBody = RequestBody.create(JsonMediaType, EMPTY)
        return buildPost(path, requestBody, headers)
    }

    fun buildPost(path: String, requestBody: RequestBody, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).post(requestBody).build()
    }

    fun buildPut(path: String, headers: Map<String, String> = emptyMap()): Request {
        val requestBody = RequestBody.create(JsonMediaType, EMPTY)
        return buildPut(path, requestBody, headers)
    }

    fun buildPut(path: String, requestBody: RequestBody, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        logger.info("the url is $url")
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).put(requestBody).build()
    }

    fun buildDelete(path: String, headers: Map<String, String> = emptyMap()): Request {
        val url = buildUrl(path)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).delete().build()
    }

    fun getJsonRequest(data: Any): RequestBody {
        return RequestBody.create(JsonMediaType, objectMapper.writeValueAsString(data))
    }

    fun encode(parameter: String): String {
        return URLEncoder.encode(parameter, "UTF-8")
    }

    private fun buildUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            if (gateway.startsWith("http://") || gateway.startsWith("https://")) {
                "$gateway/${path.removePrefix("/")}"
            } else {
                "http://$gateway/${path.removePrefix("/")}"
            }
        }
    }

    private fun getAllHeaders(headers: Map<String, String>): Map<String, String> {
        val args = buildArgs.plus(headers)
        return if (BuildEnv.getBuildType() == BuildType.AGENT) {
            val buildInfo = ThirdPartyAgentBuildInfoUtils.getBuildInfo()
            if (buildInfo == null) {
                args
            } else {
                args.plus(AUTH_HEADER_DEVOPS_BUILD_ID to buildInfo.buildId)
                    .plus(AUTH_HEADER_DEVOPS_VM_SEQ_ID to buildInfo.vmSeqId)
            }
        } else {
            args
        }
    }

    fun encodeProperty(str: String): String {
        return str.replace(",", "%5C,")
            .replace("\\", "%5C\\")
            .replace("|", "%5C|")
            .replace("=", "%5C=")
    }

    fun purePath(destPath: String): String {
        return Paths.get(
            destPath.removeSuffix("/")
                .replace("./", "/")
                .replace("../", "/")
                .replace("//", "/")
        ).toString().replace("\\", "/") // 保证win/Unix平台兼容性统一转为/分隔文件路径
    }
}