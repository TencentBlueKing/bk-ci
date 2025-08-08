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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.enums.LogStatus
import com.tencent.devops.common.log.pojo.enums.LogType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.gpt.constant.GptMessageCode.GPT_BUSY
import com.tencent.devops.gpt.constant.GptMessageCode.SCRIPT_ERROR_ANALYSIS_CHAT_TASK_LOGS_EMPTY
import com.tencent.devops.gpt.constant.GptMessageCode.SCRIPT_ERROR_ANALYSIS_CHAT_TASK_NOT_FAILED
import com.tencent.devops.gpt.constant.GptMessageCode.SCRIPT_ERROR_ANALYSIS_CHAT_TASK_NOT_FIND
import com.tencent.devops.gpt.constant.GptMessageCode.SCRIPT_ERROR_ANALYSIS_CHAT_TASK_UNDEFINED
import com.tencent.devops.gpt.dao.AIScoreDao
import com.tencent.devops.gpt.pojo.AIScoreRes
import com.tencent.devops.gpt.service.config.GptGatewayCondition
import com.tencent.devops.gpt.service.processor.ScriptErrorAnalysisProcessor
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import java.util.concurrent.TimeUnit
import org.glassfish.jersey.server.ChunkedOutput
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(GptGatewayCondition::class)
class LLMService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val llmModelService: LLMModelService,
    private val redisOperation: RedisOperation,
    private val aIScoreDao: AIScoreDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(LLMService::class.java)
        private const val CHUNK = 500
        private const val REDIS_KEY = "llm_out_cache:"

        fun label4scriptErrorAnalysisScore(
            projectId: String,
            pipelineId: String,
            buildId: String,
            taskId: String,
            executeCount: Int
        ) = "scriptErrorAnalysis.$projectId.$pipelineId.$buildId.$taskId.$executeCount"
    }

    fun llmRedisCacheKey(
        label: String
    ) = "$REDIS_KEY$label"

    fun scriptErrorAnalysisChat(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        refresh: Boolean?,
        output: ChunkedOutput<String>
    ) {
        if (taskId == "undefined") {
            output.write(I18nUtil.getCodeLanMessage(SCRIPT_ERROR_ANALYSIS_CHAT_TASK_UNDEFINED))
            return
        }
        logger.info("scriptErrorAnalysisChat|$userId|$projectId|$pipelineId|$buildId|$taskId|$executeCount|$refresh")
        // 拿插件执行信息
        val task = client.get(ServicePipelineTaskResource::class).getTaskBuildDetail(
            projectId = projectId, buildId = buildId, taskId = taskId, stepId = null, executeCount = null
        ).data ?: run {
            output.write(I18nUtil.getCodeLanMessage(SCRIPT_ERROR_ANALYSIS_CHAT_TASK_NOT_FIND))
            return
        }
        // 校验插件状态
        if (task.status != BuildStatus.FAILED && task.executeCount == executeCount) {
            output.write(I18nUtil.getCodeLanMessage(SCRIPT_ERROR_ANALYSIS_CHAT_TASK_NOT_FAILED))
            return
        }
        val label = label4scriptErrorAnalysisScore(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
        val cache = redisOperation.get(llmRedisCacheKey(label))
        if (refresh != true && cache != null) {
            logger.info("read form cache")
            output.write(cache)
            return
        }

        aIScoreDao.archive(dslContext, label)
        val processor = ScriptErrorAnalysisProcessor(output)
        // 拿脚本内容
        val ele = JsonUtil.mapTo(task.taskParams, Element::class.java)
        val script = processor.getTaskScript(ele) ?: return

        // 第一阶段：通过脚本插件落库的错误信息（提取自错误流，有长度限制）
        if (!task.errorMsg.isNullOrBlank()) {
            llmModelService.scriptErrorAnalysisChat(script, task.errorMsg!!.lines(), processor)
        }
        if (cacheInSucceed(label, processor)) return
        // 第二阶段：通过拿error log日志
        scriptErrorAnalysisChatByLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            output = output,
            script = script,
            processor = processor,
            logType = LogType.ERROR
        )
        if (cacheInSucceed(label, processor)) return
        // 第三阶段：拿全部日志

        scriptErrorAnalysisChatByLog(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount,
            output = output,
            script = script,
            processor = processor
        )
        if (!cacheInSucceed(label, processor)) {
            output.write(I18nUtil.getCodeLanMessage(GPT_BUSY))
            logger.info("scriptErrorAnalysisChat GPT_BUSY")
            return
        }
    }

    private fun scriptErrorAnalysisChatByLog(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        output: ChunkedOutput<String>,
        script: List<String>,
        processor: ScriptErrorAnalysisProcessor,
        logType: LogType? = null
    ) {
        val logsData = client.get(ServiceLogResource::class).getInitLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = taskId,
            logType = logType,
            containerHashId = null,
            executeCount = executeCount,
            jobId = null,
            stepId = null,
            reverse = true
        ).data

        if (logsData?.status != LogStatus.SUCCEED.status || logsData.logs.isEmpty()) {
            output.write(I18nUtil.getCodeLanMessage(SCRIPT_ERROR_ANALYSIS_CHAT_TASK_LOGS_EMPTY))
            return
        }
        llmModelService.scriptErrorAnalysisChat(
            script,
            logsData.logs.takeLast(CHUNK).reversed().map { it.message }.dropLast(1),
            processor
        )
    }

    fun scriptErrorAnalysisScore(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int,
        score: Boolean
    ) {
        logger.info("scriptErrorAnalysisScore|$userId|$projectId|$pipelineId|$buildId|$executeCount|$score")
        val label = label4scriptErrorAnalysisScore(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
        val redisLock = RedisLock(redisOperation, label, 10)
        redisLock.use {
            redisLock.lock()
            val record = aIScoreDao.fetchAny(
                dslContext = dslContext,
                label = label
            )
            val recordGood = record?.goodUsers?.ifBlank { null }
                ?.split(",")?.toMutableSet() ?: mutableSetOf()
            val recordBad = record?.badUsers?.ifBlank { null }
                ?.split(",")?.toMutableSet() ?: mutableSetOf()
            when {
                score -> recordGood.add(userId)
                !score -> recordBad.add(userId)
            }
            when {
                score && userId in recordBad -> recordBad.remove(userId)
                !score && userId in recordGood -> recordGood.remove(userId)
            }
            if (record != null) {
                /* update */
                aIScoreDao.updateUsers(
                    dslContext = dslContext,
                    id = record.id,
                    goodUserIds = recordGood,
                    badUserIds = recordBad
                )
            } else {
                /* create */
                val cacheKey = llmRedisCacheKey(label)
                val cacheAiMsg = redisOperation.get(cacheKey)
                val cachePushSystemMsg = redisOperation.get("$cacheKey:system")
                val cachePushUserMsg = redisOperation.get("$cacheKey:user")
                aIScoreDao.create(
                    dslContext = dslContext,
                    label = label,
                    aiMsg = cacheAiMsg ?: "",
                    systemMsg = cachePushSystemMsg ?: "",
                    userMsg = cachePushUserMsg ?: "",
                    goodUserIds = recordGood,
                    badUserIds = recordBad
                )
            }
            return
        }
    }

    fun scriptErrorAnalysisScoreGet(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): AIScoreRes {
        logger.info("scriptErrorAnalysisScoreGet|$userId|$projectId|$pipelineId|$buildId|$taskId|$executeCount")
        val label = label4scriptErrorAnalysisScore(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
        return aIScoreDao.fetchAny(
            dslContext = dslContext,
            label = label
        )?.let {
            AIScoreRes(
                goodUsers = it.goodUsers.ifBlank { null }?.split(",")?.toSet() ?: emptySet(),
                badUsers = it.badUsers.ifBlank { null }?.split(",")?.toSet() ?: emptySet()
            )
        } ?: AIScoreRes(
            goodUsers = emptySet(),
            badUsers = emptySet()
        )
    }

    fun scriptErrorAnalysisScoreDel(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int
    ) {
        logger.info("scriptErrorAnalysisScoreDel|$userId|$projectId|$pipelineId|$buildId|$taskId|$executeCount")
        val redisLock = RedisLock(
            redisOperation, label4scriptErrorAnalysisScore(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                taskId = taskId,
                executeCount = executeCount
            ), 10
        )
        val label = label4scriptErrorAnalysisScore(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount
        )
        redisLock.use {
            redisLock.lock()
            val record = aIScoreDao.fetchAny(
                dslContext = dslContext,
                label = label
            ) ?: return
            val recordGood = record.goodUsers?.ifBlank { null }
                ?.split(",")?.toMutableSet() ?: mutableSetOf()
            val recordBad = record.badUsers?.ifBlank { null }
                ?.split(",")?.toMutableSet() ?: mutableSetOf()
            recordGood.remove(userId)
            recordBad.remove(userId)
            aIScoreDao.updateUsers(
                dslContext = dslContext,
                id = record.id,
                goodUserIds = recordGood,
                badUserIds = recordBad
            )
        }
    }

    fun cacheInSucceed(
        label: String,
        processor: ScriptErrorAnalysisProcessor
    ): Boolean {
        if (processor.checkSucceed()) {
            val record = aIScoreDao.fetchAny(
                dslContext = dslContext,
                label = label
            )
            if (record != null) {
                aIScoreDao.updateMsg(
                    dslContext = dslContext,
                    id = record.id,
                    aiMsg = processor.aiMsg.toString(),
                    systemMsg = processor.getPushSystemMsg(),
                    userMsg = processor.getPushUserMsg()
                )
            }

            redisOperation.set(
                "${llmRedisCacheKey(label)}:system",
                processor.getPushSystemMsg(),
                expiredInSecond = TimeUnit.HOURS.toSeconds(1),
                expired = true
            )
            redisOperation.set(
                "${llmRedisCacheKey(label)}:user",
                processor.getPushUserMsg(),
                expiredInSecond = TimeUnit.HOURS.toSeconds(1),
                expired = true
            )
            redisOperation.set(
                llmRedisCacheKey(label),
                processor.aiMsg.toString(),
                expiredInSecond = TimeUnit.HOURS.toSeconds(1),
                expired = true
            )
            return true
        }
        return false
    }
}
