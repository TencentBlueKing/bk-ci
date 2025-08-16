package com.tencent.devops.process.yaml

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.trigger.scm.WebhookTriggerBuildService
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.yaml.exception.hanlder.YamlTriggerExceptionUtil
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.process.yaml.mq.PipelineYamlFileExecutorEvent
import com.tencent.devops.process.yaml.mq.PipelineYamlFileSchedulerEvent
import com.tencent.devops.process.yaml.pojo.PipelineYamlExecutorLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlFileExecutor @Autowired constructor(
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val pipelineYamlFileManager: PipelineYamlFileManager,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineYamlDiffService: PipelineYamlDiffService
) {

    fun execute(event: PipelineYamlFileExecutorEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|Start to execute yaml file|$eventId|$projectId|${repository.repoHashId}|$filePath"
            )
            val lock = PipelineYamlExecutorLock(
                redisOperation = redisOperation,
                projectId = projectId,
                eventId = eventId,
                filePath = filePath
            )
            try {
                if (!lock.tryLock()) {
                    logger.info("[PAC_PIPELINE] executor|$projectId|$eventId|$filePath|YamlExecutorLock try lock fail")
                    retry()
                    return
                }
                val yamlDiff = pipelineYamlDiffService.getYamlDiff(
                    projectId = projectId,
                    eventId = eventId,
                    filePath = filePath
                )
                if (yamlDiff == null || yamlDiff.status.isFinish()) {
                    logger.info(
                        "[PAC_PIPELINE] executor|$projectId|$eventId|$filePath|REPEAT_EVENT|${yamlDiff?.status}"
                    )
                    return
                }
                val yamlFileEvent = PipelineYamlFileEvent(
                    repository = repository,
                    yamlDiff = yamlDiff
                )
                yamlFileEvent.doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineYamlFileExecutorEvent.retry() {
        logger.info("pipeline yaml executor|$projectId|$eventId|$filePath|RETRY_TO_EXECUTOR_LOCK")
        this.delayMills = DEFAULT_DELAY
        sampleEventDispatcher.dispatch(this)
    }

    private fun PipelineYamlFileEvent.doHandle() {
        val context = WebhookTriggerContext(
            projectId = projectId,
            pipelineId = filePath,
            eventId = eventId
        )
        try {
            logger.info(
                "[PAC_PIPELINE]|Start to handle yaml file event|$eventId|" +
                        "$projectId|${repository.repoHashId}$filePath|$ref|$commitId|$blobId|$actionType"
            )
            when (actionType) {
                YamlFileActionType.SYNC -> {
                    sync()
                }

                YamlFileActionType.CREATE, YamlFileActionType.UPDATE -> {
                    createOrUpdateYamlFile()
                }

                YamlFileActionType.DELETE -> {
                    deleteYamlFile()
                }

                YamlFileActionType.RENAME -> {
                    renameYamlFile()
                }

                YamlFileActionType.TRIGGER -> {
                    webhookTriggerBuildService.yamlTrigger(this)
                }

                else -> Unit
            }
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                status = YamDiffFileStatus.SUCCESS
            )
        } catch (ignored: Exception) {
            logger.error(
                "[PAC_PIPELINE]|Failed to handle yaml file event|$eventId|" +
                        "$projectId|${repository.repoHashId}|$filePath|$ref|$commitId|$blobId|$actionType",
                ignored
            )
            webhookTriggerManager.fireError(context = context, exception = ignored)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                status = YamDiffFileStatus.FAILED
            )
        }
    }

    private fun PipelineYamlFileEvent.sync() {
        try {
            pipelineYamlFileManager.createOrUpdateYamlFile(this)
            pipelineYamlSyncService.syncSuccess(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
        } catch (ignored: Exception) {
            val (reason, reasonDetail) = YamlTriggerExceptionUtil.getReasonDetail(exception = ignored)
            pipelineYamlSyncService.syncFailed(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                reason = reason,
                reasonDetail = reasonDetail
            )
        }
    }

    private fun PipelineYamlFileEvent.createOrUpdateYamlFile() {
        pipelineYamlFileManager.createOrUpdateYamlFile(this)
        if (fileType.canExecute()) {
            webhookTriggerBuildService.yamlTrigger(this)
        }
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.deleteYamlFile() {
        pipelineYamlFileManager.deleteYamlFile(event = this)
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.renameYamlFile() {
        pipelineYamlFileManager.renameYamlFile(event = this)
        if (fileType.canExecute()) {
            webhookTriggerBuildService.yamlTrigger(this)
        }
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.sendSchedulerEvent() {
        sampleEventDispatcher.dispatch(
            PipelineYamlFileSchedulerEvent(
                projectId = projectId,
                repository = repository,
                eventId = eventId,
                filePath = filePath,
                fileType = fileType
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFileExecutor::class.java)
        private const val DEFAULT_DELAY = 1000
    }
}
