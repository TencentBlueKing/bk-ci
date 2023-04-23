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

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_HTTP_RESPONSE_BODY_TOO_LARGE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import okhttp3.ConnectionPool
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.util.FileCopyUtils
import java.io.CharArrayWriter
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.servlet.http.HttpServletResponse

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

    fun doLongGet(url: String, headers: Map<String, String> = mapOf()): Response {
        return doGet(longHttpClient, url, headers)
    }

    fun doLongHttp(request: Request): Response {
        return doHttp(longHttpClient, request)
    }

    fun doShortHttp(request: Request): Response {
        return doHttp(shortOkHttpClient, request)
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
        val body = RequestBody.create(jsonMediaType, jsonParam)
        val request = builder.post(body).build()
        return doHttp(request)
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
        val fileBody = RequestBody.create(octetStream, uploadFile)
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

    fun downloadFile(url: String, destPath: File, headers: Map<String, String>? = null) {
        val request = if (headers == null) {
            Request.Builder().url(url).get().build()
        } else {
            Request.Builder().url(url).headers(headers.toHeaders()).get().build()
        }
        longHttpClient.newCall(request).execute().use { response ->
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
}
