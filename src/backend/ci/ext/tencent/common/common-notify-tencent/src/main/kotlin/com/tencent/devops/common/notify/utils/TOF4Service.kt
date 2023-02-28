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
import com.tencent.devops.common.notify.pojo.EmailNotifyPost
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
class TOF4Service @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val CONTENT_TYPE = "application/json; charset=utf-8"
        private val logger = LoggerFactory.getLogger(TOF4Service::class.java)

        // TOF4网关地址
        const val TOF4_EMAIL_URL_WITH_ATTACH = "/ebus/tof4_msg/api/v1/Message/SendMail"
        const val TOF4_EMAIL_URL = "/ebus/tof4_msg/api/v1/Message/SendMailInfo"
        const val TOF4_RTX_URL = "/ebus/tof4_msg/api/v1/Message/SendRTXInfo"
    }

    private val okHttpClient = OkHttpClient()
    private val random = Random()
    private val decoder = BASE64Decoder()

    /**
     * 发送企业微信或邮件通知
     */
    fun post(url: String, postData: Any, tofConfig: Map<String, String>): TOFResult {
        val body: String
        try {
            body = objectMapper.writeValueAsString(postData)
        } catch (e: JsonProcessingException) {
            logger.error(String.format("TOF error, post tof data cannot serialize, url: %s", url), e)
            return TOFResult("TOF error, post tof data cannot serialize")
        }

        val requestBody = RequestBody.create(CONTENT_TYPE.toMediaTypeOrNull(), body)
        val headers = generateHeaders(tofConfig["paasId"]!!, tofConfig["token"]!!)
            ?: return TOFResult("TOF error, generate signature failure")
        logger.info("[$url] Start to request tof with body size: ${body.length}")

        val finalUrl = tofConfig["host"]!! + url
        var responseBody = ""

        try {
            val request = Request.Builder()
                .url(finalUrl)
                .post(requestBody)
                .headers(headers)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                responseBody = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error(
                        "TOF error, post data response failure, url: $finalUrl, status code: ${response.code}," +
                                " errorMsg: $responseBody, request body: $body"
                    )

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
            logger.error("TOF error, server response serialize failure, url: $url, response: $responseBody", e)
            return TOFResult("TOF error, server response serialize failure")
        }
    }

    /**
     * 发送带附件的邮件
     */
    fun postCodeccEmailFormData(
        url: String,
        postData: EmailNotifyPost,
        tofConfig: Map<String, String>
    ): TOFResult {
        if (postData.to.isBlank()) {
            logger.warn("TOF invalid argument, email receivers is empty")
            return TOFResult("TOF invalid argument, email receivers is empty")
        }

        val headers = generateHeaders(tofConfig["paasId"]!!, tofConfig["token"]!!)
            ?: return TOFResult("TOF error, generate signature failure")

        val params = mapOf(
            "EmailType" to postData.emailType.toString(),
            "To" to postData.to,
            "CC" to postData.cc,
            "Bcc" to postData.bcc,
            "From" to postData.from,
            "Content" to postData.content,
            "Title" to postData.title,
            "Priority" to postData.priority,
            "BodyFormat" to postData.bodyFormat.toString()
        )

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
        val finalUrl = tofConfig["host"]!! + TOF4_EMAIL_URL_WITH_ATTACH
        try {
            val taskRequest = Request.Builder()
                .url(finalUrl)
                .headers(headers)
                .post(taskBody.build())
                .build()

            OkhttpUtils.doHttp(taskRequest).use { response ->
                responseBody = response.body!!.string()
                logger.info(
                    "post codecc email to tof with url, request, response: $finalUrl \n " +
                            "$params \n $responseBody"
                )
                if (!response.isSuccessful) {
                    logger.error(
                        String.format(
                            "TOF error, post data response failure, url: %s, status code: %d, errorMsg: %s",
                            url,
                            response.code,
                            responseBody
                        )
                    )

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
            logger.error("TOF error, server response serialize failure, url: $url, response: $responseBody", e)
            return TOFResult("TOF error, server response serialize failure")
        }
    }

    /**
     * 生成TOF请求头
     * @return TOF请求头对象
     */
    private fun generateHeaders(tofPaasId: String, tofToken: String): Headers? {
        val timestamp = System.currentTimeMillis().toString().substring(0, 10)
        val nonce = random.nextInt().toString()
        val signData = String.format("%s%s%s%s", timestamp, tofToken, nonce, timestamp)
        val signature: String
        try {
            signature = HashUtils.sha256(signData)
        } catch (e: Exception) {
            logger.error("sha256 fail when generate tof4 header", e)
            return null
        }

        val headerMap = HashMap<String, String>()
        headerMap.apply {
            put("x-rio-paasid", tofPaasId)
            put("x-rio-signature", signature)
            put("x-rio-timestamp", timestamp)
            put("x-rio-nonce", nonce)
        }

        return headerMap.toHeaders()
    }
}
