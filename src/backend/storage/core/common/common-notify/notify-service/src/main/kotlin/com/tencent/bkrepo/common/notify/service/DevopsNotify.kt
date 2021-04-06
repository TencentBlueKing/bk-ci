/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.notify.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.notify.api.NotifyService
import com.tencent.bkrepo.common.notify.pojo.BaseMessage
import com.tencent.bkrepo.common.notify.pojo.DevopsResult
import com.tencent.bkrepo.common.notify.pojo.EmailNotifyMessage
import com.tencent.bkrepo.common.notify.pojo.RtxNotifyMessage
import com.tencent.bkrepo.common.notify.pojo.SmsNotifyMessage
import com.tencent.bkrepo.common.notify.pojo.WechatNotifyMessage
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 蓝盾通知服务
 */
class DevopsNotify constructor(
    val devopsServer: String
) : NotifyService {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .writeTimeout(30L, TimeUnit.SECONDS)
        .build()

    override fun sendMail(receivers: List<String>, ccs: List<String>, title: String, body: String) {
        val url = "${getServer()}/notify/api/service/notifies/email"
        val message = EmailNotifyMessage(
            receivers = receivers.toMutableSet(),
            cc = receivers.toMutableSet(),
            title = title,
            body = body
        )
        postMessage(url, message)
    }

    override fun sendSms(receivers: List<String>, body: String) {
        val url = "${getServer()}/notify/api/service/notifies/sms"
        val message = SmsNotifyMessage(
            receivers = receivers.toMutableSet(),
            body = body
        )
        postMessage(url, message)
    }

    override fun sendWework(receivers: List<String>, title: String, body: String) {
        val url = "${getServer()}/notify/api/service/notifies/rtx"
        val message = RtxNotifyMessage(
            receivers = receivers.toMutableSet(),
            title = title,
            body = body
        )
        postMessage(url, message)
    }

    override fun sendWechat(receivers: List<String>, body: String) {
        val url = "${getServer()}/notify/api/service/notifies/wechat"
        val message = WechatNotifyMessage(
            receivers = receivers.toMutableSet(),
            body = body
        )
        postMessage(url, message)
    }

    private fun postMessage(url: String, message: BaseMessage) {
        val requestContent = JsonUtils.objectMapper.writeValueAsString(message)
        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent)
        val request = Request.Builder().url(url).post(requestBody).build()
        okHttpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("send message failed code ${response.code()}, response $responseContent")
                throw RuntimeException("send message failed")
            }
            val resultData = JsonUtils.objectMapper.readValue<DevopsResult<Boolean>>(responseContent)
            if (resultData.isNotOk() || !resultData.data!!) {
                throw RuntimeException("send message failed")
            }
        }
    }

    private fun getServer(): String {
        return if (devopsServer.startsWith("http://") || devopsServer.startsWith("https://")) {
            devopsServer.removeSuffix("/")
        } else {
            "http://${devopsServer.removeSuffix("/")}"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevopsNotify::class.java)
    }
}
