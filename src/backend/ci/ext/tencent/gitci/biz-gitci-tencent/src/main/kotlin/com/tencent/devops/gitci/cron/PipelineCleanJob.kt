package com.tencent.devops.gitci.cron

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.gitci.v2.service.GitCIEventService
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 清理`T_GIT_PIPELINE_RESOURCE` 三个月前更新的流水线
 */
@Component
class PipelineCleanJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitCISettingDao: GitCIBasicSettingDao,
    private val client: Client,
    private val gitCIEventService: GitCIEventService
) {

    @Value("\${deletePipelines.minusDays:#{null}}")
    val minusDays: Long? = null

    @Scheduled(cron = "0 0 3 * * ?")
    fun cleanBuilds() {
        logger.info("[cleanPipelines] Start to clean pipelines")
        val redisLock = RedisLock(redisOperation, CLEAN_PIPELINE_JOB_REDIS_KEY, 20)
        try {
            val lockSuccess = redisLock.tryLock()
            if (!lockSuccess) {
                logger.info("[cleanPipelines] The other process is processing clean job")
                return
            }
            clean()
            logger.info("[cleanPipelines] Finish clean pipelines")
        } catch (t: Throwable) {
            logger.warn("[cleanPipelines] Fail to clean pipelines", t)
        } finally {
            redisLock.unlock()
        }
    }

    private fun clean() {
        logger.info("[cleanPipelines] Cleaning the pipelines")
        // 拿到3个月没有更新过流水线的ID进行删除
        val days = minusDays ?: 90
        val lastUpdateTime = LocalDateTime.now().withNano(0).minusDays(days)
        while (true) {
            val records = gitPipelineResourceDao.getLastUpdatePipelines(dslContext, 10, lastUpdateTime)
            if (records.isEmpty()) {
                logger.info("[cleanPipelines] The record is empty")
                return
            }

            val pipelines = records.map { Triple(it.id, it.pipelineId, it.gitProjectId) }.toSet()

            if (pipelines.isEmpty()) {
                logger.info("[cleanPipelines] Done cleaning the pipelines")
                return
            }
            logger.info("[cleanPipelines] The pipelines[$pipelines] need to be cleaned")
            cleanInDB(pipelines)
        }
    }

    private fun cleanInDB(pipelines: Set<Triple<Long, String, Long>>) {
        if (pipelines.isEmpty()) {
            return
        }
        val ids = pipelines.map { it.first }.toSet()
        val pipelineIds = pipelines.map { it.second }.toSet()
        val pipelineAndProjectIds = pipelines.map { Pair(it.second, it.third) }

        val pipelineCnt = gitPipelineResourceDao.deleteLastUpdatePipelines(dslContext, ids)
        logger.info("[cleanPipelines][$pipelineCnt] Delete the pipelines")
        val (buildCnt, notBuildCnt) = gitCIEventService.deletePipelineBuildHistory(pipelineIds)
        logger.info("[cleanPipelines][$buildCnt] Delete the builds [$notBuildCnt] Delete the not builds")

        val processClient = client.get(ServicePipelineResource::class)
        pipelineAndProjectIds.forEach {
            val gitProjectConf = gitCISettingDao.getSetting(dslContext, it.second)
            if (gitProjectConf == null) {
                logger.warn("pipelineCleanJob git ci projectCode not exist projectid: ${it.second}")
                return@forEach
            }
            processClient.delete("pipelineCleanJob", gitProjectConf.projectCode!!, it.first, ChannelCode.GIT)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCleanJob::class.java)
        private const val CLEAN_PIPELINE_JOB_REDIS_KEY = "gitci:clean:pipeline:job:lock:key"
    }
}
