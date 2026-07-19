package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.DISABLE_PAC_EVENT_DESC
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.ENABLE_PAC_EVENT_DESC
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDiff
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPacDisableReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPacEnableReq
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.yaml.common.YamlFileUtils
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.scm.api.enums.ContentKind
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * PAC 生命周期编排入口。
 *
 * 负责仓库粒度的 enable/disable 同步编排；单文件事件驱动的 YAML 生命周期变更见 [PipelineYamlFileManager]。
 */
@Service
class PipelineYamlPacManager @Autowired constructor(
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val pipelineYamlViewService: PipelineYamlViewService,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val sampleEventDispatcher: SampleEventDispatcher
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlPacManager::class.java)
    }

    /**
     * PAC 开启后,同步默认分支 CI 目录下所有 yaml 文件
     */
    fun enablePac(
        userId: String,
        projectId: String,
        yamlPacEnableReq: PipelineYamlPacEnableReq
    ) {
        val repoHashId = yamlPacEnableReq.repository.repoHashId!!
        try {
            val yamlDiffs = mutableListOf<PipelineYamlDiff>()
            val eventTime = LocalDateTime.now()
            with(yamlPacEnableReq) {
                val requestId = MDC.get(TraceTag.BIZID)
                val eventId = pipelineTriggerEventService.getEventId()
                val eventDesc = I18Variable(
                    code = ENABLE_PAC_EVENT_DESC,
                    params = listOf(userId)
                ).toJsonStr()
                val triggerEvent = PipelineTriggerEvent(
                    projectId = repository.projectId,
                    eventId = eventId,
                    triggerType = repository.getScmType().name,
                    eventSource = repository.repoHashId,
                    eventType = PipelineTriggerType.MANUAL.name,
                    triggerUser = userId,
                    eventDesc = eventDesc,
                    requestId = requestId,
                    createTime = LocalDateTime.now(),
                    eventBody = null
                )
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)

                fileTrees.filter {
                    it.kind == ContentKind.FILE && YamlFileUtils.checkYamlPipelineFile(it.path)
                }.forEach { tree ->
                    val filePath = YamlFileUtils.getCiFilePath(tree.path)
                    val oldFilePath = null
                    val yamlFileEvent = PipelineYamlDiff(
                        projectId = projectId,
                        eventId = eventId,
                        eventType = PipelineTriggerType.MANUAL.name,
                        repoHashId = repoHashId,
                        defaultBranch = defaultBranch,
                        filePath = filePath,
                        fileType = YamlFileType.getFileType(filePath),
                        actionType = YamlFileActionType.SYNC,
                        triggerUser = userId,
                        oldFilePath = oldFilePath,
                        ref = defaultBranch,
                        blobId = tree.blobId,
                        commitId = commit.sha,
                        commitMsg = commit.message,
                        commitTime = commit.commitTime ?: LocalDateTime.now(),
                        committer = commit.committer?.name ?: ""
                    )
                    yamlDiffs.add(yamlFileEvent)
                }
                val directories = yamlDiffs.map { YamlFileUtils.getCiDirectory(it.filePath) }.toSet()
                // 创建yaml流水线组
                pipelineYamlViewService.createYamlViewIfAbsent(
                    userId = userId,
                    projectId = projectId,
                    repoHashId = repoHashId,
                    aliasName = repository.aliasName,
                    directoryList = directories
                )
                yamlDiffs.forEach {
                    val yamlFileEvent = PipelineYamlFileEvent(
                        repository = repository,
                        yamlDiff = it,
                        eventTime = eventTime
                    )
                    sampleEventDispatcher.dispatch(yamlFileEvent)
                }
            }
        } catch (exception: Exception) {
            logger.error("Failed to enable pac|$projectId|$repoHashId", exception)
            pipelineYamlSyncService.enablePacFailed(
                projectId = projectId,
                repoHashId = repoHashId
            )
            throw exception
        }
    }

    /**
     * PAC 关闭:删除所有关联的 PAC 流水线
     *
     * 1. 拉取仓库下所有 yaml 流水线记录
     * 2. 建 MANUAL 触发事件(DISABLE_PAC_EVENT_DESC)
     * 3. 遍历派发 PipelineYamlFileEvent(actionType=DELETE, ref=defaultBranch),异步删除
     * 4. 立即删除 yaml 同步记录(fire-and-forget)
     */
    fun disablePac(
        userId: String,
        projectId: String,
        yamlPacDisableReq: PipelineYamlPacDisableReq
    ) {
        val repository = yamlPacDisableReq.repository
        val repoHashId = repository.repoHashId!!
        val requestDefaultBranch = yamlPacDisableReq.defaultBranch?.takeIf { it.isNotBlank() }
        logger.info(
            "[PAC_PIPELINE]|disable pac|$userId|$projectId|$repoHashId|$requestDefaultBranch"
        )
        val yamlPipelines = pipelineYamlService.getAllYamlPipeline(
            projectId = projectId,
            repoHashId = repoHashId
        )
        if (yamlPipelines.isEmpty()) {
            pipelineYamlSyncService.delete(projectId = projectId, repoHashId = repoHashId)
            return
        }

        val requestId = MDC.get(TraceTag.BIZID)
        val eventId = pipelineTriggerEventService.getEventId()
        val eventDesc = I18Variable(
            code = DISABLE_PAC_EVENT_DESC,
            params = listOf(userId)
        ).toJsonStr()
        val triggerEvent = PipelineTriggerEvent(
            projectId = projectId,
            eventId = eventId,
            triggerType = repository.getScmType().name,
            eventSource = repoHashId,
            eventType = PipelineTriggerType.MANUAL.name,
            triggerUser = userId,
            eventDesc = eventDesc,
            requestId = requestId,
            createTime = LocalDateTime.now(),
            eventBody = null
        )
        pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)

        val eventTime = LocalDateTime.now()
        yamlPipelines.forEach { info ->
            val defaultBranch = requestDefaultBranch
                ?: info.defaultBranch?.takeIf { it.isNotBlank() }
            if (defaultBranch == null) {
                logger.warn(
                    "[PAC_PIPELINE]|disable pac skip, defaultBranch empty|" +
                        "$projectId|$repoHashId|${info.filePath}|${info.pipelineId}"
                )
                return@forEach
            }
            val yamlDiff = PipelineYamlDiff(
                projectId = projectId,
                eventId = eventId,
                eventType = PipelineTriggerType.MANUAL.name,
                repoHashId = repoHashId,
                defaultBranch = defaultBranch,
                filePath = info.filePath,
                fileType = YamlFileType.getFileType(info.filePath),
                actionType = YamlFileActionType.DELETE,
                triggerUser = userId,
                oldFilePath = null,
                ref = defaultBranch,
                blobId = null,
                commitId = null,
                commitMsg = null,
                commitTime = null,
                committer = null
            )
            sampleEventDispatcher.dispatch(
                PipelineYamlFileEvent(
                    repository = repository,
                    yamlDiff = yamlDiff,
                    eventTime = eventTime
                )
            )
        }
        pipelineYamlSyncService.delete(projectId = projectId, repoHashId = repoHashId)
    }
}
