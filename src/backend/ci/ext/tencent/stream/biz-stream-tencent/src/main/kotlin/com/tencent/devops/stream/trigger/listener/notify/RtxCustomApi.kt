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

package com.tencent.devops.stream.trigger.listener.notify

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.stream.trigger.pojo.rtxCustom.MarkDown
import com.tencent.devops.stream.trigger.pojo.rtxCustom.MessageType
import com.tencent.devops.stream.trigger.pojo.rtxCustom.Receiver
import com.tencent.devops.stream.trigger.pojo.rtxCustom.ReceiverType
import com.tencent.devops.stream.trigger.pojo.rtxCustom.RtxMarkdownMessage
import com.tencent.devops.stream.trigger.pojo.rtxCustom.RtxTokenResponse
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

object RtxCustomApi {

    private val logger = LoggerFactory.getLogger(RtxCustomApi::class.java)

    fun getAccessToken(
        urlPrefix: String?,
        corpId: String?,
        corpSecret: String?
    ): String {
        if (urlPrefix.isNullOrBlank() || corpId.isNullOrBlank() || corpSecret.isNullOrBlank()) {
            throw RuntimeException(
                "RtxCustomApi get AccessToken urlPrefix|corpId|corpSecret is null"
            )
        }

        val url = "$urlPrefix/cgi-bin/gettoken?corpid=$corpId&corpSecret=$corpSecret"
        OkhttpUtils.doGet(url).use { resp ->
            if (!resp.isSuccessful) throw RuntimeException(
                "RtxCustomApi get AccessToken error code: ${resp.code()} messge: ${resp.message()}"
            )
            val responseStr = resp.body()!!.string()
            val responseData: RtxTokenResponse = jacksonObjectMapper().readValue(responseStr)
            return responseData.accessToken
        }
    }

    fun sendGitCIFinishMessage(
        urlPrefix: String?,
        token: String,
        messageType: MessageType,
        receiverType: ReceiverType,
        receiverId: String,
        content: String
    ) {
        logger.info("sendGitCIFinishMessage to $receiverId")
        if (urlPrefix.isNullOrBlank()) {
            throw RuntimeException(
                "RtxCustomApi send Stream finish message error"
            )
        }
        val url = "$urlPrefix/cgi-bin/tencent/chat/send?access_token=$token"
        val message = when (messageType) {
            MessageType.MARKDOWN -> {
                RtxMarkdownMessage(
                    receiver = Receiver(
                        type = receiverType.value,
                        id = receiverId
                    ),
                    markdown = MarkDown(
                        content = content
                    )
                )
            }
        }
        val requestBody = jacksonObjectMapper().writeValueAsString(message)
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            .build()
        OkhttpUtils.doHttp(request).use { resp ->
            if (!resp.isSuccessful) throw RuntimeException(
                "RtxCustomApi send Stream finish message error code: ${resp.code()} messge: ${resp.message()}"
            )
        }
    }
}
