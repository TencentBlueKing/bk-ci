package com.tencent.devops.common.sdk.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.sdk.SdkRequest
import com.tencent.devops.common.sdk.enums.HttpMethod
import com.tencent.devops.common.sdk.exception.SdkException
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.lang.reflect.ParameterizedType
import java.util.concurrent.TimeUnit

object SdkHttpUtil {
    private const val connectTimeout = 5L
    private const val readTimeout = 30L
    private const val writeTimeout = 30L
    private val logger = LoggerFactory.getLogger(SdkHttpUtil::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeout, TimeUnit.SECONDS)
        .readTimeout(readTimeout, TimeUnit.SECONDS)
        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        .build()

    fun buildGet(url: String, headers: Map<String, String>? = null): Request {
        return build(url, headers).get().build()
    }

    fun buildGet(url: String, headers: Map<String, String>? = null, params: Map<String, String>? = null): Request {
        val urlParams = params?.map { entry -> "${entry.key}=${entry.value}" }?.joinToString("&")
        val targetUrl = if (url.contains("?")) {
            "$url&$urlParams"
        } else {
            "$url?$urlParams"
        }
        return build(targetUrl, headers).get().build()
    }

    fun buildPost(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).post(requestBody).build()
    }

    fun buildPatch(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).patch(requestBody).build()
    }

    fun buildDelete(url: String, requestBody: RequestBody, headers: Map<String, String>? = null): Request {
        return build(url, headers).delete(requestBody).build()
    }

    fun build(url: String, headers: Map<String, String>? = null): Request.Builder {
        val builder = Request.Builder().url(url)
        if (headers != null) {
            builder.headers(Headers.of(headers))
        }
        return builder
    }

    fun request(request: Request): String {
        val response = okHttpClient.newCall(request).execute()
        return response.use { resp ->
            val responseContent = resp.body()?.string() ?: ""
            if (!resp.isSuccessful) {
                logger.error(
                    "Fail to request(${request.url()})" +
                        " with code ${resp.code()} message ${resp.message()} and response $responseContent"
                )
                throw SdkException(errCode = resp.code(), errMsg = responseContent)
            }
            responseContent
        }
    }

    /**
     * 生成post请求体对象
     */
    fun generaRequestBody(jsonStr: String, mediaType: String = "application/json"): RequestBody {
        return RequestBody.create(MediaType.parse(mediaType), jsonStr)
    }

    /**
     * 执行请求
     *
     * @param apiUrl 请求url
     * @param systemHeaders 系统请求头,所有接口都依赖的公共请求头
     * @param systemParams 系统请求参数,所有接口都依赖的公共请求参数
     * @param request 用户请求参数对象
     */
    fun <T> execute(
        apiUrl: String,
        systemHeaders: Map<String, String> = mapOf(),
        systemParams: Map<String, String> = mapOf(),
        request: SdkRequest<T>
    ): T {
        val headers = mutableMapOf<String, String>()
        headers.putAll(systemHeaders)
        headers.putAll(request.getHeaderMap())

        val params = mutableMapOf<String, String>()
        val requestParams =
            SdkJsonUtil.fromJson(SdkJsonUtil.toJson(request), object : TypeReference<Map<String, String>>() {})
        params.putAll(systemParams)
        params.putAll(requestParams)
        params.putAll(request.getUdfParams())


        val finalUrl = "${apiUrl.removeSuffix("/")}/${request.getApiPath().removePrefix("/")}"
        logger.info(
            "request $finalUrl by headers(${SdkJsonUtil.toJson(headers)}), params:${SdkJsonUtil.toJson(params)}"
        )
        val httpRequest = when (request.getHttpMethod()) {
            HttpMethod.GET ->
                buildGet(url = finalUrl, headers = headers, params = params)
            HttpMethod.POST -> {
                val requestBody = generaRequestBody(jsonStr = SdkJsonUtil.toJson(params))
                buildPost(url = finalUrl, requestBody = requestBody, headers = headers)
            }
            HttpMethod.PUT -> {
                val requestBody = generaRequestBody(jsonStr = SdkJsonUtil.toJson(params))
                buildPatch(url = finalUrl, requestBody = requestBody, headers = headers)
            }
            HttpMethod.DELETE -> {
                val requestBody = generaRequestBody(jsonStr = SdkJsonUtil.toJson(params))
                buildDelete(url = finalUrl, requestBody = requestBody, headers = headers)
            }
        }
        val actualType = (request.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
        return SdkJsonUtil.fromJson(request(httpRequest), actualType)
    }
}
