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
 *
 */

package com.tencent.devops.process.yaml.mq

import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.process.trigger.scm.WebhookTriggerBuildService
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.yaml.PipelineYamlBuildService
import com.tencent.devops.process.yaml.PipelineYamlFileManager
import com.tencent.devops.process.yaml.PipelineYamlRepositoryService
import com.tencent.devops.process.yaml.PipelineYamlSyncService
import com.tencent.devops.process.yaml.actions.EventActionFactory
import com.tencent.devops.process.yaml.exception.hanlder.YamlTriggerExceptionHandler
import com.tencent.devops.process.yaml.exception.hanlder.YamlTriggerExceptionUtil
import com.tencent.devops.process.yaml.pojo.CheckType
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlTriggerListener @Autowired constructor(
    private val actionFactory: EventActionFactory,
    private val pipelineYamlRepositoryService: PipelineYamlRepositoryService,
    private val pipelineYamlSyncService: PipelineYamlSyncService,
    private val pipelineYamlBuildService: PipelineYamlBuildService,
    private val exceptionHandler: YamlTriggerExceptionHandler,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val pipelineYamlFileManager: PipelineYamlFileManager
) : EventListener<BasePipelineYamlEvent> {
    override fun execute(event: BasePipelineYamlEvent) {
        when (event) {
            is PipelineYamlEnableEvent -> {
                enablePac(projectId = event.projectId, event = event)
            }

            is PipelineYamlTriggerEvent -> {
                trigger(projectId = event.projectId, event = event)
            }

            else -> {
                logger.warn("pipeline yaml trigger listener|not support pac yaml event|$event")
            }
        }
    }

    private fun enablePac(projectId: String, event: PipelineYamlEnableEvent) {
        logger.info("receive enable pac|$projectId|${event.actionSetting}")
        val action = try {
            val action = actionFactory.loadManualEvent(
                eventStr = event.eventStr,
                actionCommonData = event.actionCommonData,
                actionContext = event.actionContext,
                actionSetting = event.actionSetting
            )
            if (action == null) {
                logger.warn("pipeline yaml trigger listener|run|$event")
                return
            }
            action
        } catch (ignored: Throwable) {
            logger.warn("Failed to load action when enable pac|$projectId", ignored)
            return
        }
        val repoHashId = action.data.setting.repoHashId
        val filePath = action.data.context.yamlFile!!.yamlPath
        try {
            pipelineYamlRepositoryService.deployYamlPipeline(projectId = projectId, action = action)
            pipelineYamlSyncService.syncSuccess(projectId = projectId, repoHashId = repoHashId, filePath = filePath)
        } catch (ignored: Exception) {
            logger.warn("Failed to sync pipeline yaml when enable pac|$projectId|$repoHashId|$filePath", ignored)
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

    private fun trigger(projectId: String, event: PipelineYamlTriggerEvent) {
        logger.info("receive pipeline yaml trigger|$projectId|${event.actionSetting.repoHashId}|${event.yamlPath}")
        val action = try {
            val action = actionFactory.loadByData(
                eventStr = event.eventStr,
                actionCommonData = event.actionCommonData,
                actionContext = event.actionContext,
                actionSetting = event.actionSetting
            )
            if (action == null) {
                logger.warn("pipeline yaml trigger listener|run|$event")
                return
            }
            action
        } catch (ignored: Throwable) {
            logger.warn("pipeline yaml trigger|load action error", ignored)
            return
        }
        exceptionHandler.handle(action = action) {
            val yamlFile = action.data.context.yamlFile!!
            val commitId = action.data.eventCommon.commit.commitId
            logger.info(
                "receive pipeline yaml trigger|$projectId|${event.actionSetting.repoHashId}|$yamlFile|$commitId"
            )
            when (yamlFile.checkType) {
                CheckType.NEED_CHECK -> {
                    pipelineYamlRepositoryService.deployYamlPipeline(projectId = projectId, action = action)
                    pipelineYamlBuildService.start(projectId = projectId, action = action, scmType = event.scmType)
                }

                CheckType.NO_NEED_CHECK ->
                    pipelineYamlBuildService.start(projectId = projectId, action = action, scmType = event.scmType)

                CheckType.NEED_DELETE ->
                    pipelineYamlRepositoryService.deleteYamlPipeline(projectId = projectId, action = action)

                CheckType.MERGED ->
                    pipelineYamlRepositoryService.deleteYamlPipeline(
                        projectId = projectId, action = action, releaseBranch = true
                    )

                else -> Unit
            }
        }
    }

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

            YamlFileActionType.CREATE, YamlFileActionType.UPDATE -> {
                pipelineYamlFileManager.createOrUpdateYamlFile(this)
                webhookTriggerBuildService.yamlTrigger(this)
            }

            YamlFileActionType.DELETE -> {
                pipelineYamlFileManager.deleteYamlFile(event = this)
            }

            YamlFileActionType.RENAME -> {
                pipelineYamlFileManager.renameYamlFile(event = this)
                webhookTriggerBuildService.yamlTrigger(this)
            }

            YamlFileActionType.TRIGGER -> {
                webhookTriggerBuildService.yamlTrigger(this)
            }

            else -> Unit
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlTriggerListener::class.java)
    }
}
