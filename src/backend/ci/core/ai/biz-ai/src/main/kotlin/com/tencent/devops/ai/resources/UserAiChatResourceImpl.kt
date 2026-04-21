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

package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.user.UserAiChatResource
import com.tencent.devops.ai.pojo.AiChatRunStatus
import com.tencent.devops.ai.service.AiChatService
import com.tencent.devops.ai.util.AiErrorMessageTranslator
import com.tencent.devops.ai.util.SseEventWriter
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import io.agentscope.core.agui.model.RunAgentInput
import io.agentscope.core.util.JsonUtils
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors

@RestResource
class UserAiChatResourceImpl @Autowired constructor(
    private val aiChatService: AiChatService
) : UserAiChatResource {

    override fun run(
        userId: String,
        body: String
    ): ChunkedOutput<String> {
        val input = JsonUtils.getJsonCodec().fromJson(
            body, RunAgentInput::class.java
        )
        val output = ChunkedOutput<String>(String::class.java)
        EXECUTOR.execute {
            try {
                output.use { out ->
                    try {
                        aiChatService.runChat(userId, input, out)
                    } catch (e: Exception) {
                        logger.warn("[AguiChat] ChunkedOutput error: {}", e.message)
                        SseEventWriter.writeErrorAndFinish(
                            out,
                            input.threadId,
                            input.runId,
                            AiErrorMessageTranslator.toFriendlyMessage(e)
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("[AguiChat] Failed to close output: {}", e.message)
            }
        }
        return output
    }

    override fun getRunStatus(
        userId: String,
        threadId: String
    ): Result<AiChatRunStatus> {
        val runId = aiChatService.getActiveRunId(threadId)
        return if (runId != null) {
            Result(AiChatRunStatus(active = true, runId = runId))
        } else {
            Result(AiChatRunStatus(active = false))
        }
    }

    override fun stopRun(
        userId: String,
        threadId: String
    ): Result<Boolean> {
        return Result(aiChatService.stopRun(threadId))
    }

    override fun reconnectStream(
        userId: String,
        threadId: String
    ): ChunkedOutput<String> {
        val output = ChunkedOutput<String>(String::class.java)
        EXECUTOR.execute {
            try {
                output.use {
                    val reconnected = aiChatService.reconnectStream(threadId, it)
                    if (!reconnected) {
                        logger.info("[AguiChat] No active run to " + "reconnect: threadId={}", threadId)
                    }
                }
            } catch (e: Exception) {
                logger.warn(
                    "[AguiChat] Reconnect error: {}",
                    e.message
                )
            }
        }
        return output
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            UserAiChatResourceImpl::class.java
        )
        private val EXECUTOR = Executors.newCachedThreadPool()
    }
}
