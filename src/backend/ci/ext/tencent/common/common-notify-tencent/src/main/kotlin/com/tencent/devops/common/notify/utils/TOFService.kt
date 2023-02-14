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
package com.tencent.devops.common.notify.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.notify.DesUtil
import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sun.misc.BASE64Decoder
import java.util.Random

@Service
@Suppress("ALL")
class TOFService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val CONTENT_TYPE = "application/json; charset=utf-8"
        private val logger = LoggerFactory.getLogger(TOFService::class.java)

        const val EMAIL_URL = "/api/v1/Message/SendMailInfo"
        const val RTX_URL = "/api/v1/Message/SendRTXInfo"
        const val SMS_URL = "/api/v1/Message/SendSMSInfo"
        const val WECHAT_URL = "/api/v1/Message/SendWeiXinInfo"
    }

    private val okHttpClient = OkHttpClient()
    private val random = Random()
    private val decoder = BASE64Decoder()

    fun post(url: String, postData: Any, tofConf: Map<String, String>): TOFResult {

        val body: String
        try {
            body = objectMapper.writeValueAsString(postData)
        } catch (e: JsonProcessingException) {
            logger.error(String.format("TOF error, post tof data cannot serialize, url: %s", url), e)
            return TOFResult("TOF error, post tof data cannot serialize")
        }

        val requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE), body)
        val headers = generateHeaders(tofConf["sys-id"] ?: "", tofConf["app-key"] ?: "")
        if (headers == null) {
            logger.error(String.format("TOF error, generate signature failure, url: %s", url))
            return TOFResult("TOF error, generate signature failure")
        }
        logger.info("[$url] Start to request tof with body size: ${body.length}")
        val finalUrl = String.format("%s%s", tofConf["host"], url)
        val request = Request.Builder()
            .url(finalUrl)
            .post(requestBody)
            .headers(headers)
            .build()
        var responseBody = ""
        try {
            okHttpClient.newCall(request).execute().use { response ->

                responseBody = response.body()!!.string()
                if (!response.isSuccessful) {
                    // logger.error("[id--${headers["timestamp"]}]request >>>> $body")
                    logger.error("TOF error, post data response failure, url: $finalUrl, status code: ${response.code()}," +
                        " errorMsg: $responseBody, request body: $body")
                    return TOFResult("TOF error, post data response failure")
                }
            }
            val result = objectMapper.readValue(responseBody, TOFResult::class.java)
            if (result.Ret != 0 || result.ErrCode != 0) {
                if (result.ErrCode == 10002) { // 接收者验证失败
                    logger.info("post email formData fail: $result")
                } else {
                    logger.error("post email formData fail: $result")
                }
            }
            return result
        } catch (e: Throwable) {
            logger.error(String.format("TOF error, server response serialize failure, url: %s, response: %s", url, responseBody), e)
            return TOFResult("TOF error, server response serialize failure")
        }
    }

    fun postCodeccEmailFormData(url: String, postData: EmailNotifyPost, tofConf: Map<String, String>): TOFResult {
        if (postData.to.isBlank()) {
            logger.warn("TOF invalid argument, email receivers is empty")
            return TOFResult("TOF invalid argument, email receivers is empty")
        }

        val headers = generateHeaders(tofConf["sys-id"] ?: "", tofConf["app-key"] ?: "")
        if (headers == null) {
            logger.error(String.format("TOF error, generate signature failure, url: %s", url))
            return TOFResult("TOF error, generate signature failure")
        }

        val params = mapOf("EmailType" to postData.emailType.toString(),
            "To" to postData.to,
            "CC" to postData.cc,
            "Bcc" to postData.bcc,
            "From" to postData.from,
            "Content" to postData.content,
            "Title" to postData.title,
            "Priority" to postData.priority,
            "BodyFormat" to postData.bodyFormat.toString())

        val taskBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("EmailType", params["EmailType"]!!)
            .addFormDataPart("To", params["To"]!!)
            .addFormDataPart("CC", params["CC"]!!)
            .addFormDataPart("Bcc", params["Bcc"]!!)
            .addFormDataPart("From", params["From"]!!)
            .addFormDataPart("Content", params["Content"]!!)
            .addFormDataPart("Title", params["Title"]!!)
            .addFormDataPart("Priority", params["Priority"]!!)
            .addFormDataPart("BodyFormat", params["BodyFormat"]!!)

        postData.codeccAttachFileContent!!.forEach { (key, value) ->
            val fileBody = RequestBody.create(MultipartBody.FORM, decoder.decodeBuffer(value))
            taskBody.addFormDataPart("file", key, fileBody)
        }

        var responseBody = ""
        try {
            val taskRequest = Request.Builder().url(String.format("%s%s", tofConf["host"], "/api/v1/Message/SendMail"))
                .headers(headers).post(taskBody.build()).build()
            OkhttpUtils.doHttp(taskRequest).use { response ->
                responseBody = response.body()!!.string()
                logger.info("post codecc email to tof with url, request, response: $url \n $params \n $responseBody")
                if (!response.isSuccessful) {
                    // logger.error("[id--${headers["timestamp"]}]request >>>> $body")
                    logger.error(String.format("TOF error, post data response failure, url: %s, status code: %d, errorMsg: %s", url, response.code(), responseBody))
                    return TOFResult("TOF error, post data response failure")
                }
            }
            val result = objectMapper.readValue(responseBody, TOFResult::class.java)
            if (result.Ret != 0 || result.ErrCode != 0) {
                if (result.ErrCode == 10002) { // 接收者验证失败
                    logger.info("post email formData fail: $result")
                } else {
                    logger.error("post email formData fail: $result")
                }
            }
            return result
        } catch (e: Throwable) {
            logger.error(String.format("TOF error, server response serialize failure, url: %s, response: %s", url, responseBody), e)
            return TOFResult("TOF error, server response serialize failure")
        }
    }

    /**
     * 生成TOF请求头
     * @return TOF请求头对象
     */
    private fun generateHeaders(tofSysId: String, tofAppKey: String): Headers? {
        // 当前时间戳
        val t = System.currentTimeMillis()
        // 不要毫秒
        val timestamp = t.toString().substring(0, 10)
        val randomNumber = this.random.nextInt()
        val data = String.format("random%dtimestamp%s", randomNumber, timestamp)
        val signatureBytes: ByteArray
        try {
            signatureBytes = DesUtil.encrypt(data, tofSysId)
        } catch (e: Exception) {
            return null
        }

        val signature = DesUtil.toHexString(signatureBytes).toUpperCase()
        val headerMap = HashMap<String, String?>()
        headerMap.apply {
            put("appkey", tofAppKey)
            put("timestamp", timestamp)
            put("random", randomNumber.toString())
            put("signature", signature)
        }
        return Headers.of(headerMap)
    }
}
