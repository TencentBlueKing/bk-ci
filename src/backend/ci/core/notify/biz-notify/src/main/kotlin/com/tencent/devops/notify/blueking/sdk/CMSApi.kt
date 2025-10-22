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
package com.tencent.devops.notify.blueking.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import com.tencent.devops.common.notify.pojo.RtxNotifyPost
import com.tencent.devops.common.notify.pojo.SmsNotifyPost
import com.tencent.devops.common.notify.pojo.VoiceNotifyPost
import com.tencent.devops.common.notify.pojo.WechatNotifyPost
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.blueking.sdk.pojo.ApiReq
import com.tencent.devops.notify.blueking.sdk.pojo.ApiResp
import com.tencent.devops.notify.blueking.sdk.pojo.NocNoticeReq
import com.tencent.devops.notify.blueking.sdk.pojo.NotifyProperties
import com.tencent.devops.notify.blueking.sdk.pojo.SendMailReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendQyWxReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendSmsReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendVoiceReq
import com.tencent.devops.notify.blueking.sdk.pojo.SendWxReq
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.EMAIL_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.NOC_NOTICE_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.RTX_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.SMS_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.VOICE_URL
import com.tencent.devops.notify.blueking.utils.NotifyService.Companion.WECHAT_URL
import com.tencent.devops.notify.constant.NotifyMessageCode
import com.tencent.devops.notify.constant.NotifyMessageCode.BK_NOTIFY_MESSAGES
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.Headers.Companion.toHeaders

class CMSApi(private val notifyProperties: NotifyProperties) {

    /**
     * 发送邮件
     */
    fun sendMail(email: EmailNotifyPost): ApiResp {
        val mailReq = with(email) {
            SendMailReq(
                sender = null,
                title = title,
                content = content,
                receiver = null,
                receiver__username = to,
                cc = null,
                cc__username = cc,
                body_format = if (bodyFormat == 0) "Text" else "Html",
                is_content_base64 = null,
                bk_username = from
            )
        }

        return doPostRequest(EMAIL_URL, mailReq)
    }

    /**
     * 发送短信
     */
    fun sendSms(smsNotifyPost: SmsNotifyPost): ApiResp {
        val smsReq = with(smsNotifyPost) {
            SendSmsReq(
                content = msgInfo,
                receiver = null,
                receiver__username = receiver,
                is_content_base64 = null,
                bk_username = sender
            )
        }

        return doPostRequest(SMS_URL, smsReq)
    }

    /**
     * 公共语音通知
     */
    @Suppress("UNUSED")
    fun nocNotice(esbReq: NocNoticeReq): ApiResp {

        return doPostRequest(NOC_NOTICE_URL, esbReq)
    }

    /**
     * 发送企业微信
     */
    fun sendQyWeixin(rtxNotifyPost: RtxNotifyPost): ApiResp {
        val rtxReq = with(rtxNotifyPost) {
            SendQyWxReq(content = msgInfo, receiver = receiver, bk_username = sender)
        }
        return doPostRequest(RTX_URL, rtxReq)
    }

    /**
     * 发送微信消息，支持微信公众号消息，及微信企业号消息
     */
    fun sendWeixin(wechatNotifyPost: WechatNotifyPost): ApiResp {
        val wechatReq = with(wechatNotifyPost) {
            SendWxReq(
                receiver = null,
                receiver__username = receiver,
                data = SendWxReq.Data(
                    heading = I18nUtil.getCodeLanMessage(
                        messageCode = BK_NOTIFY_MESSAGES,
                        language = I18nUtil.getLanguage(wechatNotifyPost.receiver)
                    ),
                    message = msgInfo
                ),
                bk_username = sender
            )
        }
        return doPostRequest(WECHAT_URL, wechatReq)
    }

    fun sendVoice(voiceNotifyPost: VoiceNotifyPost): ApiResp {
        val voiceReq = with(voiceNotifyPost) {
            SendVoiceReq(
                auto_read_message = content,
                receiver__username = receiver,
                bk_username = "蓝鲸助手"
            )
        }
        return doPostRequest(VOICE_URL, voiceReq)
    }

    private val logger = LoggerFactory.getLogger(CMSApi::class.java)

    /**
     * 执行post请求
     */
    private fun doPostRequest(uri: String, body: ApiReq): ApiResp {
        body.bk_app_code = notifyProperties.appCode!!
        body.bk_app_secret = notifyProperties.appSecret!!

        val jsonBody = ObjectMapper().writeValueAsString(body)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val url = notifyProperties.bkHost!! + uri
        logger.info("notify post url: $url")
//        logger.info("notify post body: $jsonBody")

        val request = Request.Builder()
            .url(url)
            .headers(body.toMap().toHeaders())
            .post(requestBody)
            .build()
        val result = this.doRequest(request)
        logger.info("notify post request result: $result")

        return result
    }

    // 处理请求结果
    private fun doRequest(request: Request): ApiResp {
        var resultBean = ApiResp()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body!!.string()
                logger.info("notify response: $responseStr")
                resultBean = ObjectMapper().readValue(responseStr, ApiResp::class.java)
            } else {
                logger.error("NOTIFY_REQUEST_FAILED|url=${request.url.toUrl()}|response=($response)")
            }
            if (!resultBean.result!!) {
                logger.error("NOTIFY_SEND_MSG_FAILED|url=${request.url.toUrl()}|message=${resultBean.message}")
            }
            return resultBean
        } catch (ignore: Exception) {
            logger.error("NOTIFY_SEND_MSG_FAILED|url=${request.url.toUrl()}|message=${ignore.message}", ignore)
            throw ErrorCodeException(
                errorCode = NotifyMessageCode.ERROR_NOTIFY_SEND_FAIL,
                defaultMessage = "notify send msg failed: ${ignore.message}"
            )
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

    @Suppress("MagicNumber")
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .sslSocketFactory(sslSocketFactory(), trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    private fun sslSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(DefaultX509TrustManager())

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }

    private class DefaultX509TrustManager : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) =
            Unit

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) =
            Unit

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    }
}
