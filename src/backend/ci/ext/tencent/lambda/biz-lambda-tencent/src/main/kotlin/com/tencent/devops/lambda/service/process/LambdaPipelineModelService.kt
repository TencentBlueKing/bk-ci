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
package com.tencent.devops.lambda.service.process

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.lambda.LambdaMessageCode.STARTUP_CONFIGURATION_MISSING
import com.tencent.devops.lambda.config.LambdaKafkaTopicConfig
import com.tencent.devops.lambda.dao.process.LambdaPipelineInfoDao
import com.tencent.devops.lambda.dao.process.LambdaPipelineModelDao
import com.tencent.devops.lambda.pojo.DataPlatPipelineInfo
import com.tencent.devops.lambda.pojo.DataPlatPipelineResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ForkJoinPool

@Service
class LambdaPipelineModelService @Autowired constructor(
    private val client: Client,
    private val kafkaClient: KafkaClient,
    private val dslContext: DSLContext,
    private val lambdaPipelineModelDao: LambdaPipelineModelDao,
    private val lambdaPipelineInfoDao: LambdaPipelineInfoDao,
    private val lambdaKafkaTopicConfig: LambdaKafkaTopicConfig
) {

    fun onModelExchange(event: PipelineModelAnalysisEvent) {
        pushPipelineResource2Kafka(event.projectId, event.pipelineId, null)

        pushPipelineInfo2Kafka(
            pipelineId = event.pipelineId,
            userId = event.userId,
            projectId = event.projectId,
            channelCode = event.channelCode
        )
    }

    fun syncPipelineInfo(minId: Long, maxId: Long): Boolean {
        logger.info("====================>> syncPipelineInfo startId: $minId, endId: $maxId")
        val pipelineInfoSet = lambdaPipelineInfoDao.getPipelineInfoList(
            dslContext = dslContext,
            maxId = maxId,
            minId = minId
        ).toMutableSet()
        val forkJoinPool = ForkJoinPool(10)
        forkJoinPool.submit {
            pipelineInfoSet.parallelStream().forEach {
                pushPipelineInfo2Kafka(it.pipelineId, it.lastModifyUser, it.projectId, it.channel)
                pushPipelineResource2Kafka(it.projectId, it.pipelineId, it.version)
            }
        }

        return true
    }

    private fun pushPipelineResource2Kafka(projectId: String, pipelineId: String, version: Int?) {
        try {
            logger.info("onModelExchange sync pipeline resource, pipelineId: $pipelineId")
            val pipelineResource = lambdaPipelineModelDao.getResModel(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
            if (pipelineResource != null) {
                val dataPlatPipelineResource = DataPlatPipelineResource(
                    washTime = LocalDateTime.now().format(dateTimeFormatter),
                    pipelineId = pipelineId,
                    version = pipelineResource.version,
                    model = pipelineResource.model,
                    creator = pipelineResource.creator,
                    createTime = pipelineResource.createTime.timestampmilli().toString()
                )
                val pipelineResourceTopic1 = lambdaKafkaTopicConfig.pipelineResourceTopic
                val pipelineResourceTopic = checkParamBlank(pipelineResourceTopic1, "pipelineResourceTopic")
                kafkaClient.send(pipelineResourceTopic, JsonUtil.toJson(dataPlatPipelineResource))
//                kafkaClient.send(KafkaTopic.LANDUN_PIPELINE_RESOURCE_TOPIC, JsonUtil.toJson(dataPlatPipelineResource))
            } else {
                logger.warn("onModelExchange sync pipeline resource failed, pipelineId: $pipelineId," +
                        " pipelineResource is null.")
            }
        } catch (e: Exception) {
            logger.warn("onModelExchange sync pipeline resource failed, pipelineId: $pipelineId", e)
        }
    }

    private fun pushPipelineInfo2Kafka(
        pipelineId: String,
        userId: String,
        projectId: String,
        channelCode: String
    ) {
        try {
            logger.info("onModelExchange sync pipelineInfo, pipelineId: $pipelineId, channelCode: $channelCode")
            val pipelineInfo = client.get(ServicePipelineResource::class).status(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.valueOf(channelCode)
            ).data
            if (pipelineInfo != null) {
                val pipelineInfoTopic = checkParamBlank(lambdaKafkaTopicConfig.pipelineInfoTopic, "pipelineInfoTopic")
                kafkaClient.send(
                    pipelineInfoTopic, JsonUtil.toJson(
                    DataPlatPipelineInfo(
                        washTime = LocalDateTime.now().format(dateTimeFormatter),
                        pipelineInfo = pipelineInfo
                    )
                )
                )
//                kafkaClient.send(KafkaTopic.LANDUN_PIPELINE_INFO_TOPIC, JsonUtil.toJson(DataPlatPipelineInfo(
//                    washTime = LocalDateTime.now().format(dateTimeFormatter),
//                    pipelineInfo = pipelineInfo
//                )))
            } else {
                logger.warn("onModelExchange sync pipelineInfo failed, pipelineId: $pipelineId, pipelineInfo is null.")
            }
        } catch (e: Exception) {
            logger.warn("onModelExchange sync pipelineInfo failed, pipelineId: $pipelineId", e)
        }
    }

    private fun checkParamBlank(param: String?, message: String): String {
        if (param.isNullOrBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                messageCode = STARTUP_CONFIGURATION_MISSING,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(message)
            ))
        }
        return param
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LambdaPipelineModelService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
