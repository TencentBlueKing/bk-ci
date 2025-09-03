package com.tencent.devops.process.yaml

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.yaml.PipelineYamlDiffDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.yaml.mq.PipelineYamlFileSchedulerEvent
import com.tencent.devops.process.yaml.pojo.PipelineYamlSchedulerLock
import com.tencent.devops.repository.pojo.Repository
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlDiffService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlDiffDao: PipelineYamlDiffDao,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val redisOperation: RedisOperation
) {

    fun saveAndSend(
        projectId: String,
        repository: Repository,
        eventId: Long,
        yamlDiffs: List<PipelineYamlDiff>
    ) {
        val lock = PipelineYamlSchedulerLock(
            redisOperation = redisOperation,
            projectId = projectId,
            eventId = eventId
        )
        try {
            lock.lock()
            pipelineYamlDiffDao.batchSave(
                dslContext = dslContext,
                yamlDiffs = yamlDiffs
            )
            val yamlFileSchedulerEvent = PipelineYamlFileSchedulerEvent(
                projectId = projectId,
                repository = repository,
                eventId = eventId
            )
            sampleEventDispatcher.dispatch(yamlFileSchedulerEvent)
        } finally {
            lock.unlock()
        }
    }

    fun listYamlDiffs(
        projectId: String,
        eventId: Long
    ): List<PipelineYamlDiff> {
        return pipelineYamlDiffDao.listYamlDiffs(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId
        )
    }

    fun getYamlDiff(
        projectId: String,
        eventId: Long,
        filePath: String,
        ref: String
    ): PipelineYamlDiff? {
        return pipelineYamlDiffDao.getYamlDiff(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            filePath = filePath,
            ref = ref
        )
    }

    fun updateStatus(
        projectId: String,
        eventId: Long,
        filePath: String,
        ref: String,
        status: YamDiffFileStatus,
        errorMsg: String? = null
    ) {
        pipelineYamlDiffDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            filePath = filePath,
            ref = ref,
            status = status,
            errorMsg = errorMsg
        )
    }
}
