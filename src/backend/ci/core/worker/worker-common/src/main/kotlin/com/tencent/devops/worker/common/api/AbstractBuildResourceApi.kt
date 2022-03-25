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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
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
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.LOG_DEBUG_FLAG
import com.tencent.devops.worker.common.api.utils.ThirdPartyAgentBuildInfoUtils
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ArchiveUtils
import io.undertow.util.StatusCodes
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import java.io.File
import java.net.ConnectException
import java.net.HttpRetryException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Suppress("ALL")
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
            val response = httpClient.newCall(request).execute()
            logger.info("Request($request) with code ${response.code()}")

            if (retryCodes.contains(response.code())) { // 网关502,503，可重试
                true
            } else {
                return response
            }
        } catch (e: UnknownHostException) { // DNS问题导致请求未到达目标，可重试
            logger.warn("UnknownHostException|request($request),error is :$e, try to retry $retryCount")
            true
        } catch (e: ConnectException) {
            logger.warn("ConnectException|request($request),error is :$e, try to retry $retryCount")
            true
        } catch (re: SocketTimeoutException) {
            if (re.message == "connect timed out" ||
                (request.method() == HttpMethod.GET.name && re.message == "timeout")
            ) {
                logger.warn("SocketTimeoutException(${re.message})|request($request), try to retry $retryCount")
                true
            } else { // 对于因为服务器的超时，不一定能幂等重试的，抛出原来的异常，外层业务自行决定是否重试
                logger.error("Fail to request($request),error is :$re", re)
                throw re
            }
        } catch (ignore: Exception) {
            logger.error("Fail to request($request),error is :$ignore", ignore)
            throw ClientException("Fail to request($request),error is:${ignore.message}")
        }

        if (retryFlag && retryCount > 0) {
            logger.warn(
                "Fail to request($request), retry after $sleepTimeMills ms"
            )
            Thread.sleep(sleepTimeMills)
            return requestForResponse(request, connectTimeoutInSec, readTimeoutInSec, writeTimeoutInSec, retryCount - 1)
        } else {
            logger.error("Fail to request($request), try to retry $DEFAULT_RETRY_TIME")
            throw HttpRetryException("Fail to request($request), try to retry $DEFAULT_RETRY_TIME", 999)
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

    protected fun download(
        request: Request,
        destPath: File,
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ) {
        var retryCount = DEFAULT_RETRY_TIME
        do {
            requestForResponse(
                request = request,
                connectTimeoutInSec = connectTimeoutInSec,
                readTimeoutInSec = readTimeoutInSec,
                writeTimeoutInSec = writeTimeoutInSec
            ).use { response ->
                retryCount = download(response, destPath, retryCount)
            }
        } while (retryCount >= 0)

    }

    private fun download(response: Response, destPath: File, retryCount: Int): Int {
        if (response.code() == StatusCodes.NOT_FOUND) {
            throw RemoteServiceException("文件不存在")
        }
        if (!response.isSuccessful) {
            LoggerService.addNormalLine(response.body()!!.string())
            throw RemoteServiceException("获取文件失败")
        }
        val dest = destPath.toPath()
        if (Files.notExists(dest.parent)) Files.createDirectories(dest.parent)
        LoggerService.addNormalLine("${LOG_DEBUG_FLAG}save file >>>> ${destPath.canonicalPath}")
        val body = response.body() ?: return -1
        val contentLength = body.contentLength()
        if (contentLength != -1L) {
            LoggerService.addNormalLine("download ${dest.fileName} " +
                ArchiveUtils.humanReadableByteCountBin(contentLength))
        }

        // body copy时可能会出现readTimeout，即便http请求已正常响应
        try {
            body.byteStream().use { bs ->
                Files.copy(bs, dest, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            logger.warn("Failed to copy download body, try to retry.")
            if (retryCount > 0) {
                return retryCount - 1
            } else {
                throw HttpRetryException("Failed to copy download body, try to retry $DEFAULT_RETRY_TIME", 999)
            }
        }
        return -1
    }

    companion object {
        val JsonMediaType = MediaType.parse("application/json; charset=utf-8")
        val OctetMediaType = MediaType.parse("application/octet-stream")
        val MultipartFormData = MediaType.parse("multipart/form-data")
        private const val EMPTY = ""
        private const val DEFAULT_RETRY_TIME = 5
        private const val sleepTimeMills = 500L
        private const val CONNECT_TIMEOUT = 5L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 60L
        private val retryCodes = arrayOf(502, 503, 504)
        val logger = LoggerFactory.getLogger(AbstractBuildResourceApi::class.java)!!
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
                else -> Unit
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
        } catch (ignore: Exception) {
            throw RemoteServiceException(ignore.message!!)
        }
    }

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit

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

    fun buildGet(
        path: String,
        headers: Map<String, String> = emptyMap(),
        useFileDevnetGateway: Boolean? = null
    ): Request {
        val url = buildUrl(path, useFileDevnetGateway)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).get().build()
    }

    fun buildPost(
        path: String,
        headers: Map<String, String> = emptyMap(),
        useFileDevnetGateway: Boolean? = null
    ): Request {
        val requestBody = RequestBody.create(JsonMediaType, EMPTY)
        return buildPost(path, requestBody, headers, useFileDevnetGateway)
    }

    fun buildPost(
        path: String,
        requestBody: RequestBody,
        headers: Map<String, String> = emptyMap(),
        useFileDevnetGateway: Boolean? = null
    ): Request {
        val url = buildUrl(path, useFileDevnetGateway)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).post(requestBody).build()
    }

    fun buildPut(
        path: String,
        headers: Map<String, String> = emptyMap(),
        useFileDevnetGateway: Boolean? = null
    ): Request {
        val requestBody = RequestBody.create(JsonMediaType, EMPTY)
        return buildPut(path, requestBody, headers, useFileDevnetGateway)
    }

    fun buildPut(
        path: String,
        requestBody: RequestBody,
        headers: Map<String, String> = emptyMap(),
        useFileDevnetGateway: Boolean? = null
    ): Request {
        val url = buildUrl(path, useFileDevnetGateway)
        return Request.Builder().url(url).headers(Headers.of(getAllHeaders(headers))).put(requestBody).build()
    }

    @Suppress("UNUSED")
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

    private fun buildUrl(path: String, useFileDevnetGateway: Boolean? = null): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else if (useFileDevnetGateway != null) {
            if (useFileDevnetGateway) {
                val fileDevnetGateway = CommonEnv.fileDevnetGateway
                fixUrl(if (fileDevnetGateway.isNullOrBlank()) gateway else fileDevnetGateway, path)
            } else {
                val fileIdcGateway = CommonEnv.fileIdcGateway
                fixUrl(if (fileIdcGateway.isNullOrBlank()) gateway else fileIdcGateway, path)
            }
        } else {
            fixUrl(gateway, path)
        }
    }

    private fun fixUrl(server: String, path: String): String {
        return if (server.startsWith("http://") || server.startsWith("https://")) {
            "$server/${path.removePrefix("/")}"
        } else {
            "http://$server/${path.removePrefix("/")}"
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
