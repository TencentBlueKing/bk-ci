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
package com.tencent.devops.lambda.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic
import com.tencent.devops.lambda.dao.LambdaPipelineModelDao
import com.tencent.devops.lambda.pojo.DataPlatPipelineInfo
import com.tencent.devops.lambda.pojo.DataPlatPipelineResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class LambdaPipelineModelService @Autowired constructor(
    private val client: Client,
    private val kafkaClient: KafkaClient,
    private val dslContext: DSLContext,
    private val lambdaPipelineModelDao: LambdaPipelineModelDao
) {

    fun onModelExchange(event: PipelineModelAnalysisEvent) {
        try {
            logger.info("onModelExchange sync pipeline resource, pipelineId: ${event.pipelineId}")
            val pipelineResource = lambdaPipelineModelDao.getResModel(dslContext, event.pipelineId)
            if (pipelineResource != null) {
                val dataPlatPipelineResource = DataPlatPipelineResource(
                    washTime = LocalDateTime.now().format(dateTimeFormatter),
                    pipelineId = event.pipelineId,
                    version = pipelineResource.version,
                    model = pipelineResource.model,
                    creator = pipelineResource.creator,
                    createTime = pipelineResource.createTime.timestampmilli().toString()
                )
                kafkaClient.send(KafkaTopic.LANDUN_PIPELINE_RESOURCE_TOPIC, JsonUtil.toJson(dataPlatPipelineResource))
            } else {
                logger.error("onModelExchange sync pipeline resource failed, pipelineId: ${event.pipelineId}, pipelineResource is null.")
            }
        } catch (e: Exception) {
            logger.error("onModelExchange sync pipeline resource failed, pipelineId: ${event.pipelineId}", e)
        }

        try {
            logger.info("onModelExchange sync pipelineInfo, pipelineId: ${event.pipelineId}")
            val pipelineInfo = client.get(ServicePipelineResource::class).status(
                userId = event.userId,
                projectId = event.projectId,
                pipelineId = event.pipelineId
            ).data
            if (pipelineInfo != null) {
                kafkaClient.send(KafkaTopic.LANDUN_PIPELINE_INFO_TOPIC, JsonUtil.toJson(DataPlatPipelineInfo(
                    washTime = LocalDateTime.now().format(dateTimeFormatter),
                    pipelineInfo = pipelineInfo
                )))
            } else {
                logger.error("onModelExchange sync pipelineInfo failed, pipelineId: ${event.pipelineId}, pipelineInfo is null.")
            }
        } catch (e: Exception) {
            logger.error("onModelExchange sync pipelineInfo failed, pipelineId: ${event.pipelineId}", e)
        }
    }

    fun syncPipelineInfo(startTime: String, endTime: String) {

    }

    companion object {
        private val logger = LoggerFactory.getLogger(LambdaPipelineModelService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
