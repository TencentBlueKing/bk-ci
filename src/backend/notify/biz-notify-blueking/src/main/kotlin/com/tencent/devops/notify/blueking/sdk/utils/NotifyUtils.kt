package com.tencent.devops.notify.blueking.sdk.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.notify.blueking.sdk.pojo.ApiReq
import com.tencent.devops.notify.blueking.sdk.pojo.ApiResp
import com.tencent.devops.notify.blueking.sdk.pojo.NotifyProperties
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Component
class NotifyUtils constructor(
    notifyProperties: NotifyProperties
) {
    private val logger = LoggerFactory.getLogger(NotifyUtils::class.java)
    private val appCode = notifyProperties.appCode!!
    private val appSecret = notifyProperties.appSecret!!
    private val host = notifyProperties.bkHost!!

    /**
     * 执行post请求
     */
    fun doPostRequest(uri: String, body: ApiReq): ApiResp {
        body.bk_app_code = appCode
        body.bk_app_secret = appSecret

        val jsonbody = ObjectMapper().writeValueAsString(body)
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonbody.toString())
        val url = host + uri
        logger.info("notify post url: $url")
        logger.info("notify post body: $jsonbody")

        val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
        val result = this.doRequest(request)
        logger.info("notify post request result: $result")

        return result
    }

    // 处理请求结果
    private fun doRequest(request: Request): ApiResp {
        var resultBean: ApiResp = ApiResp()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body()!!.string()
                logger.info("notify response: $responseStr")
                resultBean = ObjectMapper().readValue(responseStr, ApiResp::class.java)
            } else {
                logger.error("notify request failed, response: ($response)")
            }
            if (!resultBean.result!!) {
                logger.error("notify send msg failed , message: ${resultBean.message}")
            }
            return resultBean
        } catch (e: Exception) {
            throw RuntimeException("notify send msg failed", e)
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

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()!!

    private fun sslSocketFactory(): SSLSocketFactory {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
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

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            return sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}