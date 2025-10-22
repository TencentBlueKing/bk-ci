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

package com.tencent.devops.gpt.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gpt.api.LLMResource
import com.tencent.devops.gpt.pojo.AIScoreRes
import com.tencent.devops.gpt.service.LLMService
import com.tencent.devops.gpt.service.config.GptGatewayCondition
import java.util.concurrent.Executors
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional

@RestResource
@Conditional(GptGatewayCondition::class)
class LLMResourceImpl @Autowired constructor(
    private val llmService: LLMService
) : LLMResource {

    companion object {
        val logger = LoggerFactory.getLogger(LLMResourceImpl::class.java)!!
    }

    private val executor = Executors.newCachedThreadPool()

    override fun scriptErrorAnalysis(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        refresh: Boolean?
    ): ChunkedOutput<String> {
        /* http/2 streaming
          *  由于jersey 设置了缓冲区ServerProperties.OUTBOUND_CONTENT_LENGTH_BUFFER
          *  所以不能使用 StreamingOutput
          *  而改用 ChunkedOutput
          * */
        val output: ChunkedOutput<String> = ChunkedOutput<String>(String::class.java)
        executor.execute {
            try {
                output.use { out ->
                    llmService.scriptErrorAnalysisChat(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        taskId = taskId,
                        executeCount = executeCount,
                        refresh = refresh,
                        output = out
                    )
                }
            } catch (ex: Exception) {
                logger.warn("scriptErrorAnalysis Chunked output error!")
            }
        }
        return output
    }

    override fun scriptErrorAnalysisScore(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        score: Boolean
    ): Result<Boolean> {
        llmService.scriptErrorAnalysisScore(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            score = score
        )
        return Result(true)
    }

    override fun scriptErrorAnalysisScoreGet(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): Result<AIScoreRes> {
        return Result(
            llmService.scriptErrorAnalysisScoreGet(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                executeCount = executeCount
            )
        )
    }

    override fun scriptErrorAnalysisScoreDel(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): Result<Boolean> {
        llmService.scriptErrorAnalysisScoreDel(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
        return Result(true)
    }
}
