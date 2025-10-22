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

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.pojo.RefreshType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class QualityCacheService @Autowired constructor(
    val redisOperation: RedisOperation
) {
    private val executors = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(5000))

    @Value("\${quality.cache.timeout:300}")
    private val redisTimeOut = 300L

    fun getCacheRuleListByPipelineId(projectId: String, pipelineId: String): List<QualityRuleMatchTask>? {
        val redisData = redisOperation.get(buildPipelineRedisKey(projectId, pipelineId))
        return buildCacheData(redisData)
    }

    fun getCacheRuleListByTemplateId(projectId: String, templateId: String): List<QualityRuleMatchTask>? {
        val redisData = redisOperation.get(buildTemplateRedisKey(projectId, templateId))
        return buildCacheData(redisData)
    }

    private fun buildCacheData(redisData: String?): List<QualityRuleMatchTask>? {
        return if (redisData == null) {
            null
        } else {
            if (redisData == "") {
                emptyList()
            } else {
                buildQuality(redisData)
            }
        }
    }

    fun buildQuality(redisData: String): List<QualityRuleMatchTask> {
        return JsonUtil.getObjectMapper().readValue(redisData)
    }

    private fun setCacheRuleListByPipeline(
        projectId: String,
        pipelineId: String,
        ruleTasks: List<QualityRuleMatchTask>?
    ) {
        if (ruleTasks == null || ruleTasks.isEmpty()) {
            redisOperation.set(buildPipelineRedisKey(projectId, pipelineId), "", redisTimeOut, true)
        } else {
            val redisData = JsonUtil.toJson(ruleTasks)
            if (redisData.length > 50000) {
                logger.warn("ruleData too long $projectId| $pipelineId| ${redisData.length}")
            }
            redisOperation.set(buildPipelineRedisKey(projectId, pipelineId), redisData, redisTimeOut, true)
        }
    }

    private fun setCacheRuleListByTemplateId(
        projectId: String,
        templateId: String,
        ruleTasks: List<QualityRuleMatchTask>?
    ) {
        if (ruleTasks == null || ruleTasks.isEmpty()) {
            redisOperation.set(buildTemplateRedisKey(projectId, templateId), "", redisTimeOut, true)
        } else {
            val redisData = JsonUtil.toJson(ruleTasks)
            if (redisData.length > 50000) {
                logger.warn("ruleData too long $projectId| $templateId| ${redisData.length}")
            }
            redisOperation.set(buildTemplateRedisKey(projectId, templateId), redisData, redisTimeOut, true)
        }
    }

    fun refreshCache(
        projectId: String,
        pipelineId: String?,
        templateId: String?,
        ruleTasks: List<QualityRuleMatchTask>?,
        type: RefreshType
    ) {
        try {
            val refreshTask = RefreshTask(
                projectId = projectId,
                pipelineId = pipelineId,
                templateId = templateId,
                ruleTasks = ruleTasks,
                cacheService = this,
                redisOperation = redisOperation,
                refreshType = type
            )
            executors.execute(refreshTask)
        } catch (e: Exception) {
            logger.warn("refreshRedis  fail: $projectId| $pipelineId| $templateId| $ruleTasks| $e")
        }
    }

    private fun buildPipelineRedisKey(projectId: String, pipelineId: String): String {
        return "$PIPELINE_RULE_KEY:$projectId:$pipelineId"
    }

    private fun buildTemplateRedisKey(projectId: String, templateId: String): String {
        return "$TEMPLATEID_RULE_KEY:$projectId:$templateId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityCacheService::class.java)
        const val PIPELINE_RULE_KEY = "rlue:pipeline:key:"
        const val TEMPLATEID_RULE_KEY = "rlue:templateId:key:"
    }

    class RefreshTask constructor(
        val projectId: String,
        val pipelineId: String?,
        val templateId: String?,
        val ruleTasks: List<QualityRuleMatchTask>?,
        val cacheService: QualityCacheService,
        val redisOperation: RedisOperation,
        val refreshType: RefreshType
    ) : Runnable {
        override fun run() {

            val redisLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = "quality:rule:lock:$projectId",
                expiredTimeInSeconds = 60
            )

            try {
                val redisRuleTasks = mutableListOf<QualityRuleMatchTask>()
                redisLock.lock()

                ruleTasks?.forEach {
                    val ruleMatchTask = QualityRuleMatchTask(
                        taskId = it.taskId,
                        taskName = it.taskName,
                        controlStage = it.controlStage,
                        ruleList = it.ruleList,
                        auditUserList = null,
                        thresholdList = null
                    )
                    redisRuleTasks.add(ruleMatchTask)
                }

                if (!pipelineId.isNullOrEmpty()) {
                    var pipelineRefresh = true
                    if (refreshType == RefreshType.GET) { // 防止多实例间 修改和查询的并发场景，以免查询的旧数据覆盖掉修改后的新数据
                        val redisPipelineData = cacheService.getCacheRuleListByPipelineId(projectId, pipelineId!!)
                        pipelineRefresh = redisPipelineData == null
                    }
                    if (pipelineRefresh) {
                        cacheService.setCacheRuleListByPipeline(projectId, pipelineId!!, redisRuleTasks)
                        logger.info("refreshRedis pipeline $projectId|$pipelineId| $ruleTasks success")
                    }
                }
                if (!templateId.isNullOrEmpty()) {
                    var templateRefresh = true

                    if (refreshType == RefreshType.GET) { // 防止多实例间 修改和查询的并发场景，以免查询的旧数据覆盖掉修改后的新数据
                        val redisTemplateData = cacheService.getCacheRuleListByTemplateId(projectId, templateId!!)
                        templateRefresh = redisTemplateData == null
                    }
                    if (templateRefresh) {
                        cacheService.setCacheRuleListByTemplateId(projectId, templateId!!, redisRuleTasks)
                        logger.info("refreshRedis template $projectId|$templateId| $ruleTasks success")
                    }
                }
            } catch (e: Exception) {
                logger.warn("refreshTask fail : $e")
            } finally {
                redisLock.unlock()
            }
        }
    }
}
