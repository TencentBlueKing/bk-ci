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

package com.tencent.devops.notify.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.notify.pojo.ImateSendMessageRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * IMate 会话消息发送客户端。
 *
 * 设计原则：
 * - 单一职责：只负责把结构化消息 POST 给 IMate 后台，不关心模板内容；
 * - 容错：调用失败仅记录日志，不向上抛异常，避免影响主流水线/审核流程（与微信/邮件 Notifier 一致）；
 * - 可扩展：通过配置文件 `notify.imate.api.url` 和 `notify.imate.api.token` 可平滑切换 IMate 后台地址与鉴权 token。
 */
@Service
class ImateService {

    @Value("\${notify.imate.api.url:}")
    private val imateApiUrl: String = ""

    @Value("\${notify.imate.api.token:}")
    private val imateApiToken: String = ""

    /**
     * 是否启用 IMate 通道：仅当配置了后台地址时启用。
     * 老环境/未对接 IMate 的部署，本字段为空，调用直接跳过，不影响历史逻辑。
     */
    fun enabled(): Boolean = imateApiUrl.isNotBlank()

    /**
     * 发送 IMate 会话消息。
     * 失败仅记录日志，吞掉异常；调用方不必担心因 IMate 异常导致流水线/审核流程被打断。
     */
    fun send(request: ImateSendMessageRequest): Boolean {
        if (!enabled()) {
            logger.warn("[IMATE] api url not configured, skip send. sessionId=${request.sessionId}")
            return false
        }
        if (request.sessionId.isBlank()) {
            logger.warn("[IMATE] sessionId is blank, skip send. scene=${request.sceneCode}")
            return false
        }
        return try {
            val body = JsonUtil.toJson(request, false)
            val headers = mutableMapOf("Content-Type" to "application/json")
            if (imateApiToken.isNotBlank()) {
                // 与 IMate 后台约定的鉴权头（双方平台预共享 token）
                headers["X-IMATE-TOKEN"] = imateApiToken
            }
            OkhttpUtils.doPost(url = imateApiUrl, jsonParam = body, headers = headers).use { response ->
                val responseStr = response.body?.string()
                if (response.isSuccessful) {
                    logger.info(
                        "[IMATE] send success. sessionId=${request.sessionId}, scene=${request.sceneCode}, " +
                            "build=${request.bizContext.buildId}"
                    )
                    true
                } else {
                    logger.warn(
                        "[IMATE] send failed. sessionId=${request.sessionId}, scene=${request.sceneCode}, " +
                            "build=${request.bizContext.buildId}, status=${response.code}, body=$responseStr"
                    )
                    false
                }
            }
        } catch (ignored: Throwable) {
            logger.warn(
                "[IMATE] send error. sessionId=${request.sessionId}, scene=${request.sceneCode}, " +
                    "build=${request.bizContext.buildId}",
                ignored
            )
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImateService::class.java)
    }
}
