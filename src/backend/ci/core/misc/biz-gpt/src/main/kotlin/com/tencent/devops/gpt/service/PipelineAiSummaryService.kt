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

package com.tencent.devops.gpt.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线AI摘要生成服务
 * 消费MQ事件后，调用AI Creative Bot生成摘要，并通过内部API写回PipelineInfo
 */
@Service
class PipelineAiSummaryService @Autowired constructor(
    private val aiCreativeBotClient: AiCreativeBotClient,
    private val client: Client
) {

    /**
     * 生成并写回AI摘要
     *
     * @param userId 操作用户
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param modelJson 流水线编排JSON
     * @param pipelineResourceVersion 流水线版本号
     */
    fun generateAndUpdateSummary(
        userId: String,
        projectId: String,
        pipelineId: String,
        modelJson: String,
        pipelineResourceVersion: Int
    ) {
        logger.info(
            "Start generating AI summary for pipeline[$pipelineId] " +
                "project[$projectId] version[$pipelineResourceVersion]"
        )

        // 1. 调用AI Creative Bot生成摘要，userId作为X-BKAIDEV-USER传入
        val summary = aiCreativeBotClient.generatePipelineSummary(userId, modelJson)
        if (summary.isNullOrBlank()) {
            logger.warn(
                "AI Creative Bot returned empty summary for pipeline[$pipelineId], skip update"
            )
            return
        }

        logger.info(
            "AI summary generated for pipeline[$pipelineId], length=${summary.length}, " +
                "updating via service API..."
        )

        // 2. 通过内部service API写回PipelineInfo的autoSummary字段（带锁和版本校验）
        try {
            val result = client.get(ServicePipelineResource::class).updateAutoSummary(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                autoSummary = summary,
                version = pipelineResourceVersion
            )
            if (result.data == true) {
                logger.info(
                    "Successfully updated autoSummary for pipeline[$pipelineId] " +
                        "version[$pipelineResourceVersion]"
                )
            } else {
                logger.warn(
                    "Failed to update autoSummary for pipeline[$pipelineId] " +
                        "version[$pipelineResourceVersion], possibly version mismatch"
                )
            }
        } catch (e: Exception) {
            logger.error(
                "Error calling updateAutoSummary API for pipeline[$pipelineId]", e
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAiSummaryService::class.java)
    }
}
