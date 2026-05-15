/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_HTTP_RESPONSE_BODY_TOO_LARGE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import java.io.CharArrayWriter
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URLEncoder
import java.net.UnknownHostException
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.servlet.http.HttpServletResponse
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.util.FileCopyUtils

@SuppressWarnings("ALL")
object OkhttpUtils {

    private val logger = LoggerFactory.getLogger(OkhttpUtils::class.java)

    val jsonMediaType = "application/json".toMediaTypeOrNull()

    private val octetStream = "application/octet-stream".toMediaTypeOrNull()

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

    private const val connectTimeout = 5L
    private const val readTimeout = 30L
    private const val writeTimeout = 30L

    init {
        logger.info("[OkhttpUtils init]")
    }

    private val shortOkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool())
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(connectTimeout, TimeUnit.SECONDS)
        .writeTimeout(connectTimeout, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    // 下载会出现从 文件源--（耗时长）---->网关（网关全部收完才转发给用户，所以用户侧与网关存在读超时的可能)-->用户
    private val longHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.MINUTES)
        .writeTimeout(readTimeout, TimeUnit.MINUTES)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    // 服务端返回301、302状态码，okhttp会把post请求转换成get请求，导致请求异常,通过followRedirects设置关闭跳转，自定义重定向
    private val redirectOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .followRedirects(false)
        .hostnameVerifier { _, _ -> true }
        .build()

    fun genOkHttpClientSupDns(host: String, ips: Set<String>) = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return if (hostname == host) {
                    ips.map { InetAddress.getByName(it) }.toList()
                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
        })
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private fun getOkHttpClientWithCustomTimeout(
        connectTimeout: Long,
        readTimeout: Long,
        writeTimeout: Long,
        followRedirects: Boolean = false
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .followRedirects(followRedirects)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    @Throws(UnsupportedEncodingException::class)
    fun joinParams(params: Map<String, String>): String {
        val paramItem = ArrayList<String>()
        for ((key, value) in params) {
            paramItem.add(key + "=" + URLEncoder.encode(value, "UTF-8"))
        }
        return paramItem.joinToString("&")
    }

    fun doGet(url: String, headers: Map<String, String> = mapOf()): Response {
        return doGet(okHttpClient, url, headers)
    }

    fun doHttp(request: Request): Response {
        return doHttp(okHttpClient, request)
    }

    fun doShortGet(url: String, headers: Map<String, String> = mapOf()): Response {
        return doGet(shortOkHttpClient, url, headers)
    }

    fun doLongGet(url: String, headers: Map<String, String> = mapOf()): Response {
        return doGet(longHttpClient, url, headers)
    }

    fun doLongHttp(request: Request): Response {
        return doHttp(longHttpClient, request)
    }

    fun doShortHttp(request: Request): Response {
        return doHttp(shortOkHttpClient, request)
    }

    fun doShortPost(url: String, jsonParam: String, headers: Map<String, String> = mapOf()): Response {
        val builder = getBuilder(url, headers)
        val body = jsonParam.toRequestBody(jsonMediaType)
        val request = builder.post(body).build()
        return doShortHttp(request)
    }

    private fun doCustomClientHttp(customOkHttpClient: OkHttpClient, request: Request): Response {
        return doHttp(customOkHttpClient, request)
    }

    fun <R> doRedirectHttp(request: Request, handleResponse: (Response) -> R): R {
        doHttp(redirectOkHttpClient, request).use { response ->
            if (
                request.method == "POST" &&
                (response.code == HttpURLConnection.HTTP_MOVED_PERM ||
                        response.code == HttpURLConnection.HTTP_MOVED_TEMP)
            ) {
                val location = response.header("Location")
                if (location != null) {
                    val newRequest = request.newBuilder().url(location).build()
                    return handleResponse(doHttp(okHttpClient, newRequest))
                }
            }
            return handleResponse(response)
        }
    }

    private fun doGet(okHttpClient: OkHttpClient, url: String, headers: Map<String, String> = mapOf()): Response {
        val requestBuilder = getBuilder(url, headers)
        val request = requestBuilder.get().build()
        return okHttpClient.newCall(request).execute()
    }

    fun doPost(url: String, jsonParam: String, headers: Map<String, String> = mapOf()): Response {
        val builder = getBuilder(url, headers)
        val body = jsonParam.toRequestBody(jsonMediaType)
        val request = builder.post(body).build()
        return doHttp(request)
    }

    fun doCustomTimeoutPost(
        connectTimeout: Long,
        readTimeout: Long,
        writeTimeout: Long,
        url: String,
        jsonParam: String,
        headers: Map<String, String> = mapOf()
    ): Response {
        val builder = getBuilder(url, headers)
        val body = jsonParam.toRequestBody(jsonMediaType)
        val request = builder.post(body).build()
        val customTimeoutOkHttpClient = getOkHttpClientWithCustomTimeout(
            connectTimeout = connectTimeout, readTimeout = readTimeout, writeTimeout = writeTimeout
        )
        return doCustomClientHttp(customTimeoutOkHttpClient, request)
    }

    private fun getBuilder(url: String, headers: Map<String, String>? = null): Request.Builder {
        val builder = Request.Builder()
        builder.url(url)
        if (headers?.isNotEmpty() == true) {
            headers.forEach { (key, value) ->
                builder.addHeader(key, value)
            }
        }
        return builder
    }

    private fun doHttp(okHttpClient: OkHttpClient, request: Request): Response {
        return okHttpClient.newCall(request).execute()
    }

    fun uploadFile(
        url: String,
        uploadFile: File,
        headers: Map<String, String?>? = null,
        fileFieldName: String = "file",
        fileName: String = uploadFile.name
    ): Response {
        val fileBody = uploadFile.asRequestBody(octetStream)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(fileFieldName, fileName, fileBody)
            .build()
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
        headers?.forEach { (key, value) ->
            if (!value.isNullOrBlank()) {
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        return doHttp(request)
    }

    fun downloadFile(
        url: String,
        destPath: File,
        headers: Map<String, String>? = null,
        connectTimeoutInSec: Long? = null,
        readTimeoutInSec: Long? = null,
        writeTimeoutInSec: Long? = null
    ) {
        val request = if (headers == null) {
            Request.Builder().url(url).get().build()
        } else {
            Request.Builder().url(url).headers(headers.toHeaders()).get().build()
        }
        val httpClient = if (connectTimeoutInSec != null || readTimeoutInSec != null || writeTimeoutInSec != null) {
            getOkHttpClientWithCustomTimeout(
                connectTimeout = connectTimeoutInSec ?: connectTimeout,
                readTimeout = readTimeoutInSec ?: readTimeout,
                writeTimeout = writeTimeoutInSec ?: writeTimeout,
                followRedirects = true
            )
        } else {
            okHttpClient
        }
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) {
                logger.warn("The file $url is not exist")
                throw RemoteServiceException("File is not exist!")
            }
            if (!response.isSuccessful) {
                logger.warn("FAIL|Download file from $url| message=${response.message}| code=${response.code}")
                throw RemoteServiceException("Get file fail")
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
        if (response.code == 304) {
            logger.info("file is newest, do not download to $destPath")
            return
        }
        if (!response.isSuccessful) {
            logger.warn("fail to download the file because of ${response.message} and code ${response.code}")
            throw RemoteServiceException("Get file fail")
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

    fun downloadFile(url: String, response: HttpServletResponse) {
        logger.info("downloadFile url is:$url")
        val httpResponse = getFileHttpResponse(url)
        FileCopyUtils.copy(httpResponse.body!!.byteStream(), response.outputStream)
    }

    fun downloadFile(url: String): javax.ws.rs.core.Response {
        val httpResponse = getFileHttpResponse(url)
        val fileName: String?
        try {
            fileName = URLEncoder.encode(File(url).name, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).build()
        }
        return javax.ws.rs.core.Response
            .ok(httpResponse.body!!.byteStream(), javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("Content-disposition", "attachment;filename=" + fileName!!)
            .header("Cache-Control", "no-cache").build()
    }

    private fun getFileHttpResponse(url: String): Response {
        val request = Request.Builder().url(url).get().build()
        val httpResponse = doLongHttp(request)
        if (!httpResponse.isSuccessful) {
            logger.error("FAIL|Download file from $url| message=${httpResponse.message}| code=${httpResponse.code}")
            throw RemoteServiceException(httpResponse.message)
        } else {
            logger.info("getFileHttpResponse isSuccessful url:$url")
        }
        return httpResponse
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

    fun Response.stringLimit(readLimit: Int, errorMsg: String? = null): String {
        val buf = CharArray(1024)
        var totalBytesRead = 0
        var len: Int
        val result = CharArrayWriter()
        body!!.charStream().use { inStream ->
            result.use { outStream ->
                while ((inStream.read(buf).also { len = it }) != -1) {
                    totalBytesRead += len
                    if (totalBytesRead >= readLimit) {
                        throw ErrorCodeException(
                            errorCode = ERROR_HTTP_RESPONSE_BODY_TOO_LARGE,
                            defaultMessage = errorMsg ?: "response body cannot be exceeded $readLimit"
                        )
                    }
                    outStream.write(buf, 0, len)
                }
                return String(outStream.toCharArray())
            }
        }
    }

    fun validUrl(url: String): Boolean {
        return try {
            url.toHttpUrl()
            true
        } catch (e: IllegalArgumentException) {
            logger.warn("url Invalid: ${e.message}")
            false
        }
    }

    fun getPort(urlStr: String): Int? {
        return try {
            val url = URL(urlStr)
            return url.port
        } catch (ignored: Exception) {
            logger.warn("url[] Invalid", ignored)
            // 处理无效URL格式的异常
            null
        }
    }

    /**
     * 始终拒绝的 host 黑名单（L1）。
     *
     * 这些 host 在任何部署形态下都没有合法的 callback 用例：
     * - `localhost` / `ip6-localhost`：回环到 bk-ci 自身后端，会暴露 actuator 等管理端点；
     * - 各云厂商元数据服务别名：用于窃取 IAM 临时凭据；
     * - `kubernetes.default*`：K8s API Server，永远不应作为业务 callback 接收方；
     * 即便管理员显式开启了 `allow-internal-url`，下列 host 仍然会被拒绝。
     */
    private val ALWAYS_BLOCKED_HOSTS = setOf(
        "metadata.google.internal",
        "metadata",
        "metadata.tencentyun.com",
        "100.100.100.200",
        "kubernetes.default.svc",
        "kubernetes.default.svc.cluster.local",
        "kubernetes.default",
        "kubernetes",
        "localhost",
        "ip6-localhost",
        "ip6-loopback"
    )

    /**
     * 始终拒绝的 host 名前缀（L1）。
     * 涵盖 169.254.0.0/16 链路本地网段（AWS/GCP/阿里云元数据服务均位于该网段）。
     */
    private val ALWAYS_BLOCKED_HOST_PREFIXES = listOf("169.254.")

    /**
     * 始终拒绝的 IP 类型（L1）：回环 / 任意本地 / 链路本地 / 组播。
     * 这些类型的地址在任何 callback 场景下都没有合法用例。
     */
    fun isAlwaysBlockedAddress(ip: InetAddress): Boolean {
        return ip.isLoopbackAddress ||
                ip.isAnyLocalAddress ||
                ip.isLinkLocalAddress ||
                ip.isMulticastAddress
    }

    /**
     * 判断 host 是否命中 L1 黑名单（始终拒绝，不受任何配置影响）。
     */
    fun isAlwaysBlockedHost(host: String): Boolean {
        if (host.isBlank()) return true
        val lowerHost = host.lowercase()
        if (ALWAYS_BLOCKED_HOSTS.contains(lowerHost)) return true
        if (ALWAYS_BLOCKED_HOST_PREFIXES.any { lowerHost.startsWith(it) }) return true
        return try {
            InetAddress.getAllByName(host).any { isAlwaysBlockedAddress(it) }
        } catch (e: UnknownHostException) {
            logger.warn("isAlwaysBlockedHost|cannot resolve host[$host]: ${e.message}")
            // 解析不到的 host 视作不可达，由调用方按实际策略处理
            false
        }
    }

    /**
     * 判断单个 [InetAddress] 是否为公网地址（既非 L1 也非站点本地 RFC1918）。
     */
    fun isPublicAddress(ip: InetAddress): Boolean {
        return !isAlwaysBlockedAddress(ip) && !ip.isSiteLocalAddress
    }

    /**
     * 校验给定的 URL 是否指向**严格意义上的公网**地址（最严格模式）。
     *
     * 规则：
     * - URL 格式合法；
     * - host 不命中 [isAlwaysBlockedHost]（L1 黑名单）；
     * - host 解析出的全部 IP 均通过 [isPublicAddress]，即不允许 RFC1918 站点本地地址。
     *
     * 适用于公网部署 / SaaS 环境。私有云部署若需要回调到 RFC1918 内网，请放开
     * `project.callback.allow-internal-url`，并在调用方分别使用 L1 检查 + [metadataSafeDns]。
     *
     * 注意：由于 DNS 解析可能在校验后被攻击者修改（DNS rebinding），调用方在执行实际 HTTP 请求时
     * 仍需配合 [ssrfSafeDns] 或 [metadataSafeDns]，不能仅依赖入库时的校验。
     */
    fun isExternalUrl(url: String): Boolean {
        return try {
            val httpUrl = url.toHttpUrl()
            isExternalHost(httpUrl.host)
        } catch (e: IllegalArgumentException) {
            logger.warn("isExternalUrl|invalid url[$url]: ${e.message}")
            false
        } catch (e: UnknownHostException) {
            logger.warn("isExternalUrl|cannot resolve url[$url]: ${e.message}")
            false
        } catch (e: Exception) {
            logger.warn("isExternalUrl|unexpected error for url[$url]: ${e.message}")
            false
        }
    }

    /**
     * 校验 host 是否为公网地址（不含 RFC1918）。规则同 [isExternalUrl]。
     */
    fun isExternalHost(host: String): Boolean {
        if (host.isBlank()) return false
        if (isAlwaysBlockedHost(host)) return false
        return try {
            val addresses = InetAddress.getAllByName(host)
            addresses.isNotEmpty() && addresses.all { isPublicAddress(it) }
        } catch (e: UnknownHostException) {
            logger.warn("isExternalHost|cannot resolve host[$host]: ${e.message}")
            false
        }
    }

    /**
     * 兼容旧调用方：判断单个 [InetAddress] 是否为公网地址（含 RFC1918 在内的所有内网均拒绝）。
     * @see isPublicAddress
     */
    @Deprecated("Use isPublicAddress instead", ReplaceWith("isPublicAddress(ip)"))
    fun isExternalAddress(ip: InetAddress): Boolean = isPublicAddress(ip)

    /**
     * 元数据安全 DNS（L1 强制保护）。
     *
     * 在每次解析时拒绝 [isAlwaysBlockedHost] 命中的目标，包括：回环、任意本地、链路本地、组播，
     * 以及云平台 / 容器编排元数据 host。**允许 RFC1918 站点本地地址**，适合私有云 / IDC 部署：
     * 业务 callback 可继续指向 `10.x` / `192.168.x` 内的服务，但永远无法被劫持到本机或元数据服务。
     *
     * 该 DNS 也提供 DNS rebinding 防御：每次请求都会重新解析并校验，攻击者无法在入库后将 host
     * 重绑定到回环或元数据地址。
     */
    fun metadataSafeDns(): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val lowerHost = hostname.lowercase()
            if (ALWAYS_BLOCKED_HOSTS.contains(lowerHost) ||
                ALWAYS_BLOCKED_HOST_PREFIXES.any { lowerHost.startsWith(it) }
            ) {
                throw UnknownHostException(
                    "host[$hostname] is blocked by SSRF protection (metadata/link-local host)"
                )
            }
            val addresses = Dns.SYSTEM.lookup(hostname)
            val blocked = addresses.filter { isAlwaysBlockedAddress(it) }
            if (blocked.isNotEmpty()) {
                throw UnknownHostException(
                    "host[$hostname] is blocked by SSRF protection " +
                            "(resolved to loopback/link-local/metadata: " +
                            "${blocked.joinToString { it.hostAddress }})"
                )
            }
            return addresses
        }
    }

    /**
     * 严格 SSRF DNS（L1 + L2，最严格）。
     *
     * 在 [metadataSafeDns] 的基础上，额外拒绝站点本地（RFC1918）地址。
     * 仅适用于面向公网的部署或明确希望禁止内网回调的环境。
     */
    fun ssrfSafeDns(): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val lowerHost = hostname.lowercase()
            if (ALWAYS_BLOCKED_HOSTS.contains(lowerHost) ||
                ALWAYS_BLOCKED_HOST_PREFIXES.any { lowerHost.startsWith(it) }
            ) {
                throw UnknownHostException(
                    "host[$hostname] is blocked by SSRF protection (metadata/link-local host)"
                )
            }
            val addresses = Dns.SYSTEM.lookup(hostname)
            val internal = addresses.filter { !isPublicAddress(it) }
            if (internal.isNotEmpty()) {
                throw UnknownHostException(
                    "host[$hostname] is blocked by SSRF protection " +
                            "(resolved to internal addresses: ${internal.joinToString { it.hostAddress }})"
                )
            }
            return addresses
        }
    }

    /**
     * 与 [okHttpClient] 配置一致，但所有出站请求均经过 [ssrfSafeDns]（L1 + L2）校验。
     * 用于面向公网的部署，禁止任何内网回调。
     */
    val ssrfSafeOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ssrfSafeDns())
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    /**
     * 与 [okHttpClient] 配置一致，但所有出站请求均经过 [metadataSafeDns]（仅 L1）校验。
     * 适用于私有云 / IDC 部署：允许 RFC1918 内网回调，但仍然禁止回环 / 链路本地 / 元数据 host。
     */
    val metadataSafeOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(metadataSafeDns())
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    /**
     * 发起一次带 SSRF 校验（L1 + L2，含 DNS pinning）的 HTTP 调用。
     * 当目标 host 解析到内网 / 元数据地址时，调用会以 [UnknownHostException] 失败。
     */
    fun doSsrfSafeHttp(request: Request): Response {
        return doHttp(ssrfSafeOkHttpClient, request)
    }

    /**
     * 发起一次仅带 L1 防护的 HTTP 调用（含 DNS pinning）：
     * 拒绝回环 / 链路本地 / 元数据 host，但允许 RFC1918 内网。
     */
    fun doMetadataSafeHttp(request: Request): Response {
        return doHttp(metadataSafeOkHttpClient, request)
    }
}
