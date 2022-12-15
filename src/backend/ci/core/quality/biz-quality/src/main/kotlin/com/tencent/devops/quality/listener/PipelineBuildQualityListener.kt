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

package com.tencent.devops.quality.listener

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCancelBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQualityReviewBroadCastEvent
import com.tencent.devops.quality.constant.MQ as QualityMQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildReviewBroadCastEvent
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.dao.v2.QualityHisMetadataDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleReviewerDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("NestedBlockDepth")
class PipelineBuildQualityListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityHistoryDao: HistoryDao,
    private val qualityRuleReviewerDao: QualityRuleReviewerDao,
    private val qualityHisMetadataDao: QualityHisMetadataDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildQualityListener::class.java)
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = QualityMQ.QUEUE_PIPELINE_BUILD_CANCEL_QUALITY, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_CANCEL_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineCancelQualityListener(pipelineCancelEvent: PipelineBuildCancelBroadCastEvent) {
        try {
            logger.info("QUALITY|pipelineCancelListener cancelEvent: ${pipelineCancelEvent.buildId}")
            val ruleIdList = qualityRuleBuildHisDao.listBuildHisRules(
                dslContext = dslContext,
                projectId = pipelineCancelEvent.projectId,
                pipelineId = pipelineCancelEvent.pipelineId,
                ruleBuildId = pipelineCancelEvent.buildId
            ).filter { it.status == RuleInterceptResult.WAIT.name }.map { it.id }
            logger.info("QUALITY|wait_rule_size: ${ruleIdList.size}")
            if (ruleIdList.isNotEmpty()) {
                qualityRuleBuildHisDao.batchUpdateStatus(ruleIdList, RuleInterceptResult.FAIL.name)
            }
        } catch (e: Exception) {
            logger.warn("QUALITY|pipelineCancelListener error: ${e.message}")
        }
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = QualityMQ.QUEUE_PIPELINE_BUILD_RETRY_QUALITY, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_QUEUE_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineRetryBroadCastEvent(pipelineRetryStartEvent: PipelineBuildQueueBroadCastEvent) {
        try {
            logger.info("QUALITY|pipelineRetryListener retryEvent: ${pipelineRetryStartEvent.buildId}")
            if (pipelineRetryStartEvent.actionType.isRetry()) {
                qualityHisMetadataDao.deleteHisMetaByBuildId(dslContext, pipelineRetryStartEvent.buildId)
                val ruleIdList = qualityRuleBuildHisDao.listBuildHisRules(
                    dslContext = dslContext,
                    projectId = pipelineRetryStartEvent.projectId,
                    pipelineId = pipelineRetryStartEvent.pipelineId,
                    ruleBuildId = pipelineRetryStartEvent.buildId
                ).map { it.id }
                logger.info("QUALITY|retry_rule_size: ${ruleIdList.size}")
                if (ruleIdList.isNotEmpty()) {
                    qualityRuleBuildHisDao.updateBuildId(ruleIdList, null)
                    qualityRuleBuildHisDao.batchUpdateStatus(ruleIdList, RuleInterceptResult.UNCHECK.name)
                }
            }
        } catch (e: Exception) {
            logger.warn("QUALITY|pipelineRetryListener error: ${e.message}")
        }
    }

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = QualityMQ.QUEUE_PIPELINE_BUILD_TIMEOUT_QUALITY, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_REVIEW_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineTimeoutBroadCastEvent(pipelineTimeoutEvent: PipelineBuildReviewBroadCastEvent) {
        try {
            logger.info("QUALITY|pipelineTimeoutListener timeoutEvent: ${pipelineTimeoutEvent.buildId}")
            val projectId = pipelineTimeoutEvent.projectId
            val pipelineId = pipelineTimeoutEvent.pipelineId
            val buildId = pipelineTimeoutEvent.buildId
            if (pipelineTimeoutEvent.timeout == true) {
                if (pipelineTimeoutEvent.source == "taskAtom") {
                    logger.info("QUALITY_TIMEOUT|projectId=[$projectId]|pipelineId=[$pipelineId]|buildId=[$buildId]")
                    qualityHistoryDao.batchUpdateHistoryResult(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        result = RuleInterceptResult.INTERCEPT_TIMEOUT,
                        ruleIds = null
                    )
                } else {
                    val ruleIdList = qualityRuleBuildHisDao.listBuildHisRules(
                        dslContext = dslContext,
                        projectId = pipelineTimeoutEvent.projectId,
                        pipelineId = pipelineTimeoutEvent.pipelineId,
                        ruleBuildId = pipelineTimeoutEvent.buildId
                    ).map { it.id }
                    logger.info("QUALITY|timeout_rule_size: ${ruleIdList.size}")
                    if (ruleIdList.isNotEmpty()) {
                        qualityRuleBuildHisDao.batchUpdateStatus(ruleIdList, RuleInterceptResult.INTERCEPT_TIMEOUT.name)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("pipelineTimeoutListener error: ${e.message}")
        }
    }

    /**
     * 蓝盾流水线质量红线人工审核广播事件
     */
    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = QualityMQ.QUEUE_PIPELINE_BUILD_QUALITY_REVIEW, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_QUALITY_REVIEW_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )
    fun listenPipelineQualityReviewBroadCastEvent(event: PipelineBuildQualityReviewBroadCastEvent) {
        try {
            logger.info("QUALITY|qualityReviewListener reviewEvent: ${event.buildId}")
            val action = if (event.reviewType == BuildReviewType.QUALITY_TASK_REVIEW_PASS)
                RuleInterceptResult.INTERCEPT_PASS else RuleInterceptResult.INTERCEPT
            val projectId = event.projectId
            val pipelineId = event.pipelineId
            val buildId = event.buildId
            val ruleIds = event.ruleIds.map { HashUtil.decodeIdToLong(it) }.toSet()
            val count = qualityHistoryDao.batchUpdateHistoryResult(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                result = action,
                ruleIds = ruleIds
            )
            logger.info("QUALITY|[${event.buildId}]history result update count: $count")

            // 保存或更新蓝盾红线审核人信息
            ruleIds.forEach { ruleId ->
                if (checkReviewExist(projectId, pipelineId, buildId, ruleId)) {
                    qualityRuleReviewerDao.update(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        ruleId = ruleId,
                        reviewer = event.userId
                    )
                } else {
                    qualityRuleReviewerDao.create(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        ruleId = ruleId,
                        reviewer = event.userId
                    )
                }
            }
            logger.info("QUALITY|[${event.buildId}]save reviewer info done.")
        } catch (e: Exception) {
            logger.warn("QUALITY|listenPipelineQualityReviewBroadCastEvent|${event.buildId}|warn=${e.message}")
        }
    }

    private fun checkReviewExist(
        projectId: String,
        pipelineId: String,
        buildId: String,
        ruleId: Long
    ): Boolean {
        val result = qualityRuleReviewerDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            ruleId = ruleId
        )
        return result != null
    }
}
