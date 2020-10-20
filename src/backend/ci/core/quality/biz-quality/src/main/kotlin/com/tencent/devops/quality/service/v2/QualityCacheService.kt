package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class QualityCacheService @Autowired constructor(
    val redisOperation: RedisOperation
) {
    private val executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    @Value("\${quality.cache.timeout:300}")
    val REDIS_TIMEOUT = 300L

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
        return JsonUtil.getObjectMapper().readValue(redisData!!)
    }

    private fun setCacheRuleListByPipeline(projectId: String, pipelineId: String, ruleTasks: List<QualityRuleMatchTask>?) {
        if (ruleTasks == null || ruleTasks.isEmpty()) {
            redisOperation.set(buildPipelineRedisKey(projectId, pipelineId), "", REDIS_TIMEOUT, true)
        } else {
            redisOperation.set(buildPipelineRedisKey(projectId, pipelineId), JsonUtil.toJson(ruleTasks), REDIS_TIMEOUT, true)
        }
    }

    private fun setCacheRuleListByTemplateId(projectId: String, templateId: String, ruleTasks: List<QualityRuleMatchTask>?) {
        if (ruleTasks == null || ruleTasks.isEmpty()) {
            redisOperation.set(buildTemplateRedisKey(projectId, templateId), "", REDIS_TIMEOUT, true)
        } else {
            redisOperation.set(buildTemplateRedisKey(projectId, templateId), JsonUtil.toJson(ruleTasks), REDIS_TIMEOUT, true)
        }
    }

    fun refreshCache(projectId: String, pipelineId: String?, templateId: String?, ruleTasks: List<QualityRuleMatchTask>?) {
        val redisLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = "quality:rule:lock:$projectId",
                expiredTimeInSeconds = 60
        )
        try {
            redisLock.lock()
            val refreshTask = RefreshTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    templateId = templateId,
                    ruleTasks = ruleTasks,
                    cacheService = this
            )
            executors.execute(refreshTask)
        } catch (e: Exception) {
            logger.warn("refreshRedis  fail: $projectId| $pipelineId| $templateId| $ruleTasks| $e")
        } finally {
            redisLock.unlock()
        }
    }

    private fun buildPipelineRedisKey(projectId: String, pipelineId: String): String {
        return "$PIPELINE_RULE_KEY:$projectId:$pipelineId"
    }

    private fun buildTemplateRedisKey(projectId: String, templateId: String): String {
        return "$TEMPLATEID_RULE_KEY:$projectId:$templateId"
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        val PIPELINE_RULE_KEY = "rlue:pipeline:key:"
        val TEMPLATEID_RULE_KEY = "rlue:templateId:key:"
    }

    class RefreshTask constructor(
            val projectId: String,
            val pipelineId: String?,
            val templateId: String?,
            val ruleTasks: List<QualityRuleMatchTask>?,
            val cacheService: QualityCacheService
    ): Runnable {
        override fun run() {
            if (!pipelineId.isNullOrEmpty()) {
                cacheService.setCacheRuleListByPipeline(projectId, pipelineId!!, ruleTasks)
                logger.info("refreshRedis pipeline $projectId|$pipelineId| $ruleTasks success")
            }
            if (!templateId.isNullOrEmpty()) {
                cacheService.setCacheRuleListByTemplateId(projectId, templateId!!, ruleTasks)
                logger.info("refreshRedis template $projectId|$templateId| $ruleTasks success")
            }
        }
    }
}