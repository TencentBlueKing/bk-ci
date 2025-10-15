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

package com.tencent.devops.process.trigger.scm

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.utils.PIPELINE_PAC_REPO_HASH_ID
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.process.yaml.mq.PipelineYamlFileEvent
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.webhook.Webhook
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebhookTriggerBuildService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildService: PipelineBuildService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val pipelineYamlService: PipelineYamlService,
    private val webhookTriggerMatcher: WebhookTriggerMatcher,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelineTriggerEventService: PipelineTriggerEventService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WebhookTriggerBuildService::class.java)
    }

    fun trigger(
        projectId: String,
        pipelineId: String,
        version: Int?,
        eventId: Long,
        repository: Repository,
        webhook: Webhook
    ) {
        val context = WebhookTriggerContext(projectId = projectId, pipelineId = pipelineId, eventId = eventId)
        try {
            val pipelineInfo =
                pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                    params = arrayOf(pipelineId)
                )
            context.pipelineInfo = pipelineInfo

            val resource = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, version)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                )
            val model = resource.model

            val variables = mutableMapOf<String, String>()
            val container = model.stages[0].containers[0] as TriggerContainer
            // 解析变量
            container.params.forEach { param ->
                variables[param.id] = param.defaultValue.toString()
            }
            // 填充[variables.]前缀
            variables.putAll(PipelineVarUtil.fillVariableMap(variables))
            if (repository.enablePac == true) {
                variables[PIPELINE_PAC_REPO_HASH_ID] = repository.repoHashId!!
            }
            val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
            container.elements.filterIsInstance<WebHookTriggerElement>().forEach elements@{ element ->
                if (!element.elementEnabled()) {
                    return@elements
                }
                val atomResponse = webhookTriggerMatcher.matches(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    repository = repository,
                    webhook = webhook,
                    variables = variables,
                    element = element
                )
                when (atomResponse.matchStatus) {
                    MatchStatus.REPOSITORY_NOT_MATCH,
                    MatchStatus.ELEMENT_NOT_MATCH,
                    MatchStatus.EVENT_TYPE_NOT_MATCH -> return@elements

                    MatchStatus.CONDITION_NOT_MATCH -> {
                        failedMatchElements.add(
                            PipelineTriggerFailedMatchElement(
                                elementId = element.id,
                                elementName = element.name,
                                elementAtomCode = element.getAtomCode(),
                                reasonMsg = atomResponse.failedReason ?: I18Variable(
                                    code = TRIGGER_CONDITION_NOT_MATCH
                                ).toJsonStr()
                            )
                        )
                    }

                    MatchStatus.SUCCESS -> {
                        startPipeline(
                            context = context,
                            pipelineInfo = pipelineInfo,
                            resource = resource,
                            startParams = atomResponse.outputVars
                        )
                        return
                    }
                }
            }
            if (failedMatchElements.isNotEmpty()) {
                context.failedMatchElements = failedMatchElements
                webhookTriggerManager.fireMatchFailed(context)
            }
        } catch (ignored: Exception) {
            logger.error(
                "Failed to trigger by webhook|" +
                    "projectId: $projectId|pipelineId: $pipelineId|repoHashId: ${repository.repoHashId}",
                ignored
            )
            webhookTriggerManager.fireError(context, ignored)
        }
    }

    fun yamlTrigger(event: PipelineYamlFileEvent) {
        with(event) {
            logger.info(
                "[PAC_PIPELINE]|Start to trigger yaml pipeline|eventId:$eventId|" +
                    "projectId: $projectId|repoHashId: $repoHashId|filePath: $filePath|" +
                    "ref: $ref|blobId: $blobId"
            )
            val triggerEvent = pipelineTriggerEventService.getTriggerEvent(projectId = projectId, eventId = eventId)
                ?: throw throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
                    params = arrayOf(eventId.toString())
                )
            val webhook = triggerEvent.eventBody ?: throw throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_BODY_NOT_FOUND,
                params = arrayOf(eventId.toString())
            )
            /*
            1. 如果是默认分支,则查找当前文件blob_id在默认分支对应的版本
            2. 如果不是默认分支
                - 查找blob_id在当前分支是否存在对应的版本
                - 如果当前分支不存在,则查找是否在默认分支存在
                - 如果默认分支也不存在,则查找所有的blob_id对应的版本,
                    这种情况出现在分支a,修改了文件,分支b从分支a拉出,后面分支a又做了修改,分支a合入默认分支后
             */
            val pipelineBranchVersion = pipelineYamlService.getPipelineYamlVersion(
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = ref,
                blobId = blobId,
                branchAction = BranchVersionAction.ACTIVE.name
            )
            val pipelineYamlVersion = if (ref == defaultBranch) {
                pipelineBranchVersion
            } else {
                pipelineBranchVersion ?: pipelineYamlService.getPipelineYamlVersion(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    ref = defaultBranch,
                    blobId = blobId,
                    branchAction = BranchVersionAction.ACTIVE.name
                ) ?: pipelineYamlService.getPipelineYamlVersion(
                    projectId = projectId,
                    repoHashId = repoHashId,
                    filePath = filePath,
                    blobId = blobId
                )
            } ?: run {
                logger.info(
                    "[PAC_PIPELINE]|trigger yaml pipeline not found pipeline version|eventId: $eventId|" +
                        "projectId: $projectId|repoHashId: $repoHashId|filePath: $filePath|blobId: $blobId"
                )
                return
            }
            logger.info(
                "[PAC_PIPELINE]|find yaml pipeline trigger version|eventId:$eventId|" +
                    "projectId: $projectId|repoHashId: $repoHashId|filePath: $filePath|" +
                    "ref: $ref|blobId: $blobId|" +
                    "pipelineId: ${pipelineYamlVersion.pipelineId}|version: ${pipelineYamlVersion.version}"
            )
            trigger(
                projectId = projectId,
                pipelineId = pipelineYamlVersion.pipelineId,
                version = pipelineYamlVersion.version,
                eventId = eventId,
                repository = repository,
                webhook = webhook
            )
        }
    }

    private fun startPipeline(
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        startParams: Map<String, Any>
    ) {
        val startEpoch = System.currentTimeMillis()
        val (projectId, pipelineId) = pipelineInfo.projectId to pipelineInfo.pipelineId
        val userId = pipelineRepositoryService.getPipelineOauthUser(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: pipelineInfo.lastModifyUser
        val buildId = pipelineBuildService.startPipeline(
            userId = userId,
            pipeline = pipelineInfo,
            startType = StartType.WEB_HOOK,
            pipelineParamMap = convertBuildParameters(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                triggerContainer = resource.model.getTriggerContainer(),
                startParams = startParams
            ),
            channelCode = pipelineInfo.channelCode,
            isMobile = false,
            resource = resource,
            signPipelineVersion = resource.version,
            frequencyLimit = false
        )
        logger.info(
            "success to trigger by webhook|eventId:${context.eventId}|" +
                "projectId: $projectId|pipelineId: $pipelineId|version: ${resource.version}"
        )
        context.buildId = buildId
        context.startParams = startParams
        webhookTriggerManager.fireBuildSuccess(context = context)
        logger.info("$pipelineId|WEBHOOK_TRIGGER|time=${System.currentTimeMillis() - startEpoch}")
    }

    private fun convertBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerContainer: TriggerContainer,
        startParams: Map<String, Any>
    ): MutableMap<String, BuildParameters> {
        val pipelineParamMap = mutableMapOf<String, BuildParameters>()
        val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            paramProperties = triggerContainer.params,
            paramValues = startParams.mapValues { it.value.toString() }
        )
        pipelineParamMap.putAll(paramMap)
        startParams.forEach {
            if (paramMap.containsKey(it.key)) {
                return@forEach
            }
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                pipelineParamMap[it.key] = BuildParameters(key = it.key, value = it.value ?: "")
            } else if (!pipelineParamMap.contains(newVarName)) { // 新变量还不存在，加入
                pipelineParamMap[newVarName] = BuildParameters(key = newVarName, value = it.value ?: "")
            }
        }
        return pipelineParamMap
    }
}
