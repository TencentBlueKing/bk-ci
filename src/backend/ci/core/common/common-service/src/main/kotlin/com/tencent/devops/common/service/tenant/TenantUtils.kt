package com.tencent.devops.common.service.tenant

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import jakarta.ws.rs.HttpMethod
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.env.get

class TenantUtils : ApplicationContextAware, InitializingBean {
    private var applicationContext: ApplicationContext? = null


    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun afterPropertiesSet() {
        val environment = applicationContext?.environment
        if (environment == null) {
            return
        }
        enableMultiTenantMode = environment["bk.enableMultiTenantMode"] == "true"
        appCode = environment["bk.apigw.appCode"] ?: ""
        appSecret = environment["bk.apigw.appSecret"] ?: ""
        disableEsb = environment["bk.apigw.disableEsb"] == "true"
    }

    companion object {
        private var enableMultiTenantMode: Boolean = false
        private var appCode = ""
        private var appSecret = ""
        private var disableEsb: Boolean = false

        private const val DEFAULT_TENANT_ID_FOR_SINGLE = "default"
        public const val DEFAULT_TENANT_ID_FOR_MULTI = "system"
        private val logger = LoggerFactory.getLogger(TenantUtils::class.java)

        fun disableEsb(): Boolean {
            return disableEsb || isMultiTenantMode()
        }

        /**
         * 是否开启多租户模式
         */
        fun isMultiTenantMode(): Boolean {
            return enableMultiTenantMode
        }

        /**
         * 获取租户id
         */
        fun getTenantId(tenantId: String? = null): String? {
            return if (!enableMultiTenantMode) {
                null
            } else if (!tenantId.isNullOrBlank()) {
                tenantId
            } else {
                DEFAULT_TENANT_ID_FOR_MULTI
            }
        }

        /**
         * 生成英文名称
         */
        fun parseEnglishName(tenantId: String? = null, tenantEnglishName: String): String {
            return if (tenantEnglishName.contains(".")) {
                tenantEnglishName
            } else if (!enableMultiTenantMode) {
                tenantEnglishName
            } else if (!tenantId.isNullOrBlank()) {
                "$tenantId.$tenantEnglishName"
            } else {
                tenantEnglishName
            }
        }

        /**
         * 根据英文名称获取租户id
         */
        fun getTenantIdByEnglishName(tenantEnglishName: String): String? {
            return if (tenantEnglishName.contains(".")) {
                tenantEnglishName.split(".")[0]
            } else if (enableMultiTenantMode) {
                DEFAULT_TENANT_ID_FOR_MULTI
            } else {
                null
            }
        }

        /**
         * 调用api网关
         */
        fun <T> callApigw(
            apigwHost: String,
            path: String,
            params: Map<String, Any>,
            tenantId: String?,
            method: String,
            respType: Class<T>
        ): T {
            var url = getAuthRequestUrl(apigwHost, path)
            var body: RequestBody? = null
            if (method == HttpMethod.GET) {
                // 将map参数拼接到url上
                url = buildString {
                    append("$url?")
                    append(params.entries.joinToString("&") { "${it.key}=${it.value}" })
                }
            } else {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                body = JsonUtil.toJson(params).toRequestBody(mediaType)
            }
            logger.info("callApi: url = $url , body = $body")
            val request = Request.Builder().url(url)
                .headers(getHeaders(tenantId))
                .let { if (method == HttpMethod.GET) it.get() else it.post(body!!) }
                .build()
            OkhttpUtils.doHttp(request).use {
                if (!it.isSuccessful) {
                    // 请求错误
                    logger.warn("call api fail: url = $url | params = $params | response = ($it)")
                    throw RuntimeException("call api fail: url = $url | params = $params | response = ($it)")
                }
                val responseStr = it.body!!.string()
                logger.info("call api : response = $responseStr")
                return JsonUtil.getObjectMapper().readValue(responseStr, respType)
            }
        }

        /**
         * 生成请求url
         */
        private fun getAuthRequestUrl(apigwHost: String, uri: String): String {
            return if (apigwHost.endsWith("/")) {
                apigwHost + uri
            } else {
                "$apigwHost/$uri"
            }
        }

        private fun getHeaders(tenantId: String?): Headers {
            val headers = Headers.Builder()
            headers.add("X-Bkapi-Authorization", "{\"bk_app_code\": \"$appCode\", \"bk_app_secret\": \"$appSecret\"}")
            headers.add("X-Bk-Tenant-Id", getTenantId(tenantId) ?: DEFAULT_TENANT_ID_FOR_SINGLE)
            return headers.build()
        }

    }
}