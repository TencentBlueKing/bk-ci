package com.tencent.devops.process.yaml

import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetailMessageCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.trigger.scm.ScmWebhookTriggerBuildService
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.yaml.exception.hanlder.YamlTriggerExceptionUtil
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlFileExecutor @Autowired constructor(
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val scmWebhookTriggerBuildService: ScmWebhookTriggerBuildService,
    private val pipelineYamlFileManager: PipelineYamlFileManager,
    private val webhookTriggerManager: WebhookTriggerManager
) {

    fun execute(event: PipelineYamlFileEvent) {
        with(event) {
            val context = WebhookTriggerContext(
                projectId = projectId,
                pipelineId = filePath,
                eventId = eventId
            )
            try {
                logger.info(
                    "[PAC_PIPELINE]|Start to handle yaml file event|eventId:$eventId|" +
                            "projectId:$projectId|repoHashId:${repository.repoHashId}|" +
                            "filePath:$filePath|ref:$ref|commitId:${commit?.commitId}|blobId:$blobId|" +
                            "actionType:$actionType"
                )
                handle()
            } catch (ignored: Exception) {
                logger.error(
                    "[PAC_PIPELINE]|Failed to handle yaml file event|eventId:$eventId|" +
                            "projectId:$projectId|repoHashId:${repository.repoHashId}|" +
                            "filePath:$filePath|ref:$ref|commitId:${commit?.commitId}|blobId:$blobId|" +
                            "actionType:$actionType",
                    ignored
                )
                webhookTriggerManager.fireError(context = context, exception = ignored)
            }
        }
    }

    private fun PipelineYamlFileEvent.handle() {
        when (actionType) {
            YamlFileActionType.SYNC -> {
                sync()
            }

            YamlFileActionType.CREATE, YamlFileActionType.UPDATE -> {
                val success = pipelineYamlFileManager.createOrUpdateYamlFile(this)
                // 只有流水线才需要触发,yaml处理失败继续触发会使用旧版本
                if (success && !isTemplate) {
                    scmWebhookTriggerBuildService.yamlTrigger(this)
                }
            }

            YamlFileActionType.DELETE -> {
                pipelineYamlFileManager.deleteYamlFile(event = this)
            }

            YamlFileActionType.RENAME -> {
                val success = pipelineYamlFileManager.renameYamlFile(event = this)
                // 只有流水线才需要触发,yaml处理失败继续触发会使用旧版本
                if (success && !isTemplate) {
                    scmWebhookTriggerBuildService.yamlTrigger(this)
                }
            }

            YamlFileActionType.TRIGGER -> {
                scmWebhookTriggerBuildService.yamlTrigger(this)
            }

            YamlFileActionType.CLOSE -> {
                pipelineYamlFileManager.closeYamlFile(this)
            }

            else -> Unit
        }
    }

    private fun PipelineYamlFileEvent.sync() {
        try {
            if (!pipelineYamlFileManager.createOrUpdateYamlFile(this)) {
                // 失败原因已由createOrUpdateYamlFile记录到触发详情,这里只能拿到通用原因
                syncFailed(
                    reason = PipelineTriggerReason.TRIGGER_FAILED.name,
                    reasonDetail = PipelineTriggerDetailMessageCode(
                        messageCode = ProcessMessageCode.BK_YAML_PIPELINE_CREATE_FAILED
                    )
                )
                return
            }
            pipelineYamlSyncService.syncSuccess(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath
            )
        } catch (ignored: Exception) {
            val (reason, reasonDetail) = YamlTriggerExceptionUtil.getReasonDetail(exception = ignored)
            syncFailed(reason = reason, reasonDetail = reasonDetail)
        }
    }

    private fun PipelineYamlFileEvent.syncFailed(
        reason: String,
        reasonDetail: PipelineTriggerReasonDetail
    ) {
        pipelineYamlSyncService.syncFailed(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            reason = reason,
            reasonDetail = reasonDetail
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlFileExecutor::class.java)
    }
}
