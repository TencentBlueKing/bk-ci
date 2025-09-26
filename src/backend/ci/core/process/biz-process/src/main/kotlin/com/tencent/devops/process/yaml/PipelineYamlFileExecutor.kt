package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.trigger.scm.WebhookTriggerBuildService
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
    private val pipelineYamlFileManager: PipelineYamlFileManager,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineYamlDiffService: PipelineYamlDiffService
) {

    fun execute(event: PipelineYamlFileExecutorEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|Start to execute yaml file|$eventId|$projectId|${repository.repoHashId}|$filePath|$ref"
            )
            val lock = PipelineYamlExecutorLock(
                redisOperation = redisOperation,
                projectId = projectId,
                eventId = eventId,
                filePath = filePath
            )
            try {
                if (!lock.tryLock()) {
                    logger.info(
                        "[PAC_PIPELINE] yaml file executor|$projectId|$eventId|$filePath|$ref|" +
                                "YamlExecutorLock try lock fail"
                    )
                    retry()
                    return
                }
                val yamlDiff = pipelineYamlDiffService.getYamlDiff(
                    projectId = projectId,
                    eventId = eventId,
                    filePath = filePath,
                    ref = ref
                )
                if (yamlDiff == null || yamlDiff.status.isFinished()) {
                    logger.info(
                        "[PAC_PIPELINE] yaml file executor|$projectId|$eventId|$filePath|$ref|" +
                                "REPEAT_EVENT|${yamlDiff?.status}"
                    )
                    return
                }
                val yamlFileEvent = PipelineYamlFileEvent(
                    repository = repository,
                    yamlDiff = yamlDiff
                )
                yamlFileEvent.doHandle()
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to execute yaml file|" +
                        "$eventId|$projectId|${repository.repoHashId}|$filePath|$ref",
                    ignored
                )
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineYamlFileExecutorEvent.retry() {
        logger.info("yaml file executor|$projectId|$eventId|$filePath|RETRY_TO_EXECUTOR_LOCK")
        this.delayMills = DEFAULT_DELAY
        sampleEventDispatcher.dispatch(this)
    }

    private fun PipelineYamlFileEvent.doHandle() {
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
                triggerYamlFile()
            }

            YamlFileActionType.DEPENDENCY_UPGRADE -> {
                dependencyUpgradeYamlFile()
            }

            YamlFileActionType.DEPENDENCY_UPGRADE_AND_TRIGGER -> {
                dependencyUpgradeAndTriggerYamlFile()
            }

            YamlFileActionType.CLOSE -> {
                closeYamlFile()
            }

            else -> Unit
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
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
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
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.FAILED
            )
        }
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.createOrUpdateYamlFile() {
        try {
            pipelineYamlFileManager.createOrUpdateYamlFile(this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
            if (fileType.canExecute()) {
                webhookTriggerBuildService.yamlTrigger(this)
            }
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to create or update yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref|" +
                        "commitId:$commitId|blobId:$blobId",
                ignored
            )
        }
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.deleteYamlFile() {
        try {
            pipelineYamlFileManager.deleteYamlFile(event = this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to delete yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                ignored
            )
        }
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.renameYamlFile() {
        try {
            pipelineYamlFileManager.renameYamlFile(event = this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
            if (fileType.canExecute()) {
                webhookTriggerBuildService.yamlTrigger(this)
            }
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to rename yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                ignored
            )
        }
        if (fileType.needNotifyScheduler()) {
            sendSchedulerEvent()
        }
    }

    private fun PipelineYamlFileEvent.triggerYamlFile() {
        try {
            webhookTriggerBuildService.yamlTrigger(this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to trigger yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                ignored
            )
        }
    }

    private fun PipelineYamlFileEvent.dependencyUpgradeAndTriggerYamlFile() {
        try {
            pipelineYamlFileManager.dependencyUpgradeYamlFile(this)
            webhookTriggerBuildService.yamlTrigger(this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to dependency upgrade yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                ignored
            )
        }
    }

    private fun PipelineYamlFileEvent.dependencyUpgradeYamlFile() {
        try {
            pipelineYamlFileManager.dependencyUpgradeYamlFile(this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to dependency upgrade yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                ignored
            )
        }
    }

    private fun PipelineYamlFileEvent.closeYamlFile() {
        try {
            pipelineYamlFileManager.closeYamlFile(this)
            pipelineYamlDiffService.updateStatus(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                status = YamDiffFileStatus.SUCCESS
            )
        } catch (ignored: Exception) {
            handleException(
                projectId = projectId,
                eventId = eventId,
                filePath = filePath,
                ref = ref,
                exception = ignored
            )
            logger.error(
                "[PAC_PIPELINE]|Failed to dependency upgrade yaml|eventId:$eventId|" +
                        "projectId:$projectId|repoHashId:$repoHashId|filePath:$filePath|ref:$ref",
                ignored
            )
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

    private fun handleException(
        projectId: String,
        eventId: Long,
        filePath: String,
        ref: String,
        exception: Exception
    ) {
        val errorMsg = when (exception) {
            is ErrorCodeException -> {
                I18nUtil.getCodeLanMessage(
                    messageCode = exception.errorCode,
                    params = exception.params
                )
            }

            else -> {
                exception.message
            }
        }
        pipelineYamlDiffService.updateStatus(
            projectId = projectId,
            eventId = eventId,
            filePath = filePath,
            ref = ref,
            status = YamDiffFileStatus.FAILED,
            errorMsg = errorMsg
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFileExecutor::class.java)
        private const val DEFAULT_DELAY = 1000
    }
}
