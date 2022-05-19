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

package com.tencent.bkrepo.webhook.executor

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.webhook.config.WebHookProperties
import com.tencent.bkrepo.webhook.constant.WEBHOOK_AUTH_HEADER
import com.tencent.bkrepo.webhook.constant.WEBHOOK_EVENT_HEADER
import com.tencent.bkrepo.webhook.constant.WebHookRequestStatus
import com.tencent.bkrepo.webhook.dao.WebHookLogDao
import com.tencent.bkrepo.webhook.model.TWebHook
import com.tencent.bkrepo.webhook.model.TWebHookLog
import com.tencent.bkrepo.webhook.payload.EventPayloadFactory
import com.tencent.bkrepo.webhook.pojo.payload.CommonEventPayload
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.cloud.endpoint.event.RefreshEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDateTime
import java.util.Locale

/**
 * WebHook回调执行
 */
@Component
class WebHookExecutor(
    private val webHookLogDao: WebHookLogDao,
    private val eventPayloadFactory: EventPayloadFactory,
    private val webHookProperties: WebHookProperties
) {

    private val httpClient = HttpClientBuilderFactory.create().build()

    init {
        httpClient.dispatcher().maxRequests = webHookProperties.maxRequests ?: 200
        httpClient.dispatcher().maxRequestsPerHost = webHookProperties.maxRequestsPerHost ?: 100
    }

    /**
     * 刷新HttpClient参数设置
     */
    @EventListener(RefreshEvent::class)
    @Order
    fun refresh(event: RefreshEvent) {
        webHookProperties.maxRequests?.let { httpClient.dispatcher().maxRequests = it }
        webHookProperties.maxRequestsPerHost?.let { httpClient.dispatcher().maxRequestsPerHost = it }
        logger.info("refresh httpClient config maxRequests: ${httpClient.dispatcher().maxRequests}," +
                "maxRequestsPerHost: ${httpClient.dispatcher().maxRequestsPerHost}")
    }

    fun execute(event: ArtifactEvent, webHook: TWebHook): TWebHookLog {
        val payload = eventPayloadFactory.build(event)
        return execute(payload, webHook)
    }

    fun execute(payload: CommonEventPayload, webHook: TWebHook): TWebHookLog {
        val request = buildRequest(webHook, payload)
        val startTimestamp = System.currentTimeMillis()
        val log = buildWebHookLog(webHook, request, payload)
        try {
            httpClient.newCall(request).execute().use {
                if (!it.isSuccessful) throw IOException("unexpected code $it")
                logger.info("Execute web hook[id=${webHook.id}, url=${webHook.url}] success.")
                buildWebHookSuccessLog(log, startTimestamp, it)
            }
        } catch (exception: IOException) {
            logger.error("Execute web hook[id=${webHook.id}, url=${webHook.url}] error. ${exception.cause}")
            buildWebHookFailedLog(log, startTimestamp, exception.message)
        }
        return webHookLogDao.insert(log)
    }

    fun asyncExecutor(event: ArtifactEvent, webHookList: List<TWebHook>) {
        webHookList.forEach { asyncExecutor(event, it) }
    }

    fun asyncExecutor(event: ArtifactEvent, webHook: TWebHook) {
        val payload = eventPayloadFactory.build(event)
        val request = buildRequest(webHook, payload)
        val startTimestamp = System.currentTimeMillis()
        val log = buildWebHookLog(webHook, request, payload)
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                logger.error("Execute webhook[id=${webHook.id}, url=${webHook.url}] error. ${exception.cause}")
                buildWebHookFailedLog(log, startTimestamp, exception.message)
                webHookLogDao.insert(log)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) throw IOException("unexpected code $it")
                        logger.info("Execute web hook[id=${webHook.id}, url=${webHook.url}] success.")
                        buildWebHookSuccessLog(log, startTimestamp, it)
                    }
                } catch (exception: IOException) {
                    logger.error("Execute web hook[id=${webHook.id}, url=${webHook.url}] error. ${exception.cause}")
                    buildWebHookFailedLog(log, startTimestamp, exception.message)
                }
                webHookLogDao.insert(log)
            }
        })
    }

    private fun buildWebHookLog(
        webHook: TWebHook,
        request: Request,
        payload: CommonEventPayload
    ): TWebHookLog {
        return TWebHookLog(
            webHookId = webHook.id!!,
            webHookUrl = webHook.url,
            triggeredEvent = payload.eventType,
            requestHeaders = request.headers().toMap(),
            requestPayload = payload.toJsonString(),
            requestDuration = 0L,
            requestTime = LocalDateTime.now(),
            status = WebHookRequestStatus.FAIL
        )
    }

    private fun buildWebHookSuccessLog(log: TWebHookLog, startTimestamp: Long, response: Response) {
        log.requestDuration = System.currentTimeMillis() - startTimestamp
        log.status = WebHookRequestStatus.SUCCESS
        log.responseHeaders = response.headers().toMap()
        log.responseBody = response.body()?.string()
    }

    private fun buildWebHookFailedLog(log: TWebHookLog, startTimestamp: Long, errorMsg: String?) {
        log.requestDuration = System.currentTimeMillis() - startTimestamp
        log.status = WebHookRequestStatus.FAIL
        log.errorMsg = errorMsg
    }

    private fun buildRequest(
        webHook: TWebHook,
        payload: CommonEventPayload
    ): Request {
        val requestBody = RequestBody.create(MediaType.parse(MediaTypes.APPLICATION_JSON), payload.toJsonString())
        val builder = Request.Builder().url(webHook.url).post(requestBody)
        builder.addHeader(WEBHOOK_EVENT_HEADER, payload.eventType.name)
        webHook.token?.let { token -> builder.addHeader(WEBHOOK_AUTH_HEADER, token) }
        return builder.build()
    }

    private fun Headers.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (name in names()) {
            val key = name.toLowerCase(Locale.US)
            val value = get(key)
            map[key] = value!!
        }
        return map
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebHookExecutor::class.java)
    }
}
