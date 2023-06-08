/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.stream.trigger

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.ResourcesPools
import com.tencent.devops.process.yaml.v2.models.format
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.context.TriggerCache
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.actions.tgit.TGitNoteActionGit
import com.tencent.devops.stream.trigger.actions.tgit.TGitReviewActionGit
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.YamlBehindException
import com.tencent.devops.stream.trigger.exception.YamlBlankException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.toStreamGitProjectInfoWithProject
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlFormat
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.pojo.MrYamlInfo
import com.tencent.devops.stream.trigger.pojo.YamlReplaceResult
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.HandsomeUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("LongParameterList", "LongMethod")
class StreamYamlTrigger @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val triggerMatcher: TriggerMatcher,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val yamlTemplateService: YamlTemplateService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val yamlBuild: StreamYamlBuild,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamYamlBaseBuild: StreamYamlBaseBuild,
    private val streamGitConfig: StreamGitConfig
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlTrigger::class.java)
        const val ymlVersion = "v2.0"

        // 针对filePath可能为空的情况下创建一个模板替换的根目录名称
        const val STREAM_TEMPLATE_ROOT_FILE = "STREAM_TEMPLATE_ROOT_FILE"
    }

    fun checkAndTrigger(
        action: BaseAction,
        trigger: String?
    ) {
        logger.info("|${action.data.context.requestEventId}|checkAndTrigger|action|${action.format()}")
        val buildPipeline = action.data.context.pipeline!!

        val filePath = buildPipeline.filePath
        // 流水线未启用则跳过
        if (!buildPipeline.enabled) {
            logger.warn(
                "Pipeline $filePath is not enabled, gitProjectId: ${action.data.eventCommon.gitProjectId}, " +
                    "eventId: ${action.data.context.requestEventId}"
            )
            throw StreamTriggerException(action, TriggerReason.PIPELINE_DISABLE)
        }

        // 使用触发缓存
        val triggerEvent = if (trigger == null) {
            null
        } else {
            val e = triggerMatcher.isMatch(action, trigger)
            logger.info(
                "${action.data.eventCommon.gitProjectId}|${action.data.context.pipeline?.pipelineId} " +
                    "use trigger cache result $trigger"
            )
            e
        }

        // 这里判断，各类注册事件如果修改blobId肯定不同，相同的肯定注册过了，所以只要不触发git就直接跳过
        if (triggerEvent != null && !triggerEvent.second.trigger.trigger) {
            logger.info(
                "${buildPipeline.pipelineId}| use trigger cache" +
                    "Matcher is false, return, gitProjectId: ${action.data.getGitProjectId()}, " +
                    "eventId: ${action.data.context.requestEventId}"
            )
            throw StreamTriggerException(
                action, TriggerReason.TRIGGER_NOT_MATCH,
                reasonParams = listOf(triggerEvent.second.trigger.notTriggerReason ?: "")
            )
        }

        val yamlContent = action.getYamlContent(filePath)
        action.data.context.originYaml = yamlContent.content

        // 如果当前文件没有内容直接不触发
        if (yamlContent.content.isBlank()) {
            throw StreamTriggerException(
                action,
                TriggerReason.CI_YAML_CONTENT_NULL,
                commitCheck = CommitCheck(
                    block = action.metaData.isStreamMr(),
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }

        // 因为mr获取文件后分支信息可能不同，这里单独重新更新触发器缓存信息
        if (action is StreamMrAction) {
            yamlContent as MrYamlInfo
            if (yamlContent.ref.isNotBlank() && !yamlContent.blobId.isNullOrBlank())
                action.data.context.triggerCache = TriggerCache(
                    pipelineFileBranch = yamlContent.ref,
                    blobId = yamlContent.blobId
                )
        }

        yamlSchemaCheck.check(action = action, templateType = null, isCiFile = true)

        // 进入触发流程
        trigger(action, triggerEvent)
    }

    // 目前无多余扩展，方便后续多版本在这里进行逻辑处理
    fun trigger(
        action: BaseAction,
        triggerEvent: Pair<List<Any>?, TriggerResult>?
    ) {
        triggerBuild(action, triggerEvent)
    }

    @Suppress("ComplexMethod")
    @BkTimed
    fun triggerBuild(
        action: BaseAction,
        triggerEvent: Pair<List<Any>?, TriggerResult>?
    ): Boolean {
        logger.info(
            "StreamYamlTrigger|triggerBuild|requestEventId" +
                "|${action.data.context.requestEventId}|action|${action.format()}"
        )
        var pipeline = action.data.context.pipeline!!

        // 提前创建新流水线，保证git提交后 stream上能看到
        if (pipeline.pipelineId.isBlank()) {
            pipeline = StreamTriggerPipeline(
                gitProjectId = action.data.getGitProjectId(),
                pipelineId = "",
                filePath = pipeline.filePath,
                displayName = getDisplayName(action),
                enabled = true,
                creator = action.data.getUserId(),
                lastUpdateBranch = action.data.eventCommon.branch
            )
            streamYamlBaseBuild.createNewPipeLine(
                pipeline = pipeline,
                projectCode = action.getProjectCode(),
                action = action
            )
            // 新建流水线放
            action.data.context.pipeline = pipeline
        } else {
            action.updatePipelineLastBranchAndDisplayName(
                pipelineId = pipeline.pipelineId,
                branch = if (needUpdateLastBuildBranch(action)) action.data.eventCommon.branch else null,
                displayName = if (needChangePipelineDisplayName(action)) getDisplayName(action) else null
            )
        }

        // 获取蓝盾流水线的pipelineAsCodeSetting
        val projectCode = GitCommonUtils.getCiProjectId(pipeline.gitProjectId.toLong(), streamGitConfig.getScmType())
        action.data.context.pipelineAsCodeSettings = try {
            if (pipeline.pipelineId.isNotBlank()) {
                client.get(ServicePipelineSettingResource::class).getPipelineSetting(
                    projectId = projectCode,
                    pipelineId = pipeline.pipelineId,
                    channelCode = ChannelCode.GIT
                ).data?.pipelineAsCodeSettings
            } else {
                client.get(ServiceProjectResource::class).get(projectCode)
                    .data?.properties?.pipelineAsCodeSettings
            }
        } catch (ignore: Throwable) {
            logger.warn("StreamYamlTrigger get project[$projectCode] as code settings error.", ignore)
            null
        }

        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态，只有url或者名称不对才更新
        val gitProjectInfo = action.api.getGitProjectInfo(
            action.getGitCred(),
            action.getGitProjectIdOrName(),
            ApiRequestRetryInfo(true)
        )!!
        action.data.setting = action.data.setting.copy(gitHttpUrl = gitProjectInfo.gitHttpUrl)

        // 前面使用缓存触发器判断过得就不用再判断了
        // 同时使用缓存触发成功的肯定不用在重复注册各类事件了
        val tr = if (triggerEvent?.second != null) {
            TriggerResult(
                trigger = triggerEvent.second.trigger,
                triggerOn = triggerEvent.second.triggerOn,
                timeTrigger = false,
                deleteTrigger = false,
                repoHookName = null
            )
        } else {
            triggerMatcher.isMatch(action)
        }
        val (isTriggerBody, _, isTiming, isDelete, repoHookName) = tr
        val (isTrigger, notTriggerReason) = isTriggerBody
        logger.info(
            "StreamYamlTrigger|triggerBuild|pipelineId|" +
                "${pipeline.pipelineId}|Match return|isTrigger=$isTrigger|" +
                "isTiming=$isTiming|isDelete=$isDelete|repoHookName=$repoHookName"
        )
        if (tr.noNeedTrigger()) {
            logger.warn(
                "StreamYamlTrigger|triggerBuild|pipelineId|" +
                    "${pipeline.pipelineId}|" +
                    "Matcher is false|gitProjectId|${action.data.getGitProjectId()}|" +
                    "eventId|${action.data.context.requestEventId}"
            )
            throw StreamTriggerException(
                action = action,
                triggerReason = TriggerReason.TRIGGER_NOT_MATCH,
                reasonParams = listOf(notTriggerReason ?: "")
            )
        }

        val startParams = GitActionCommon.getStartParams(action, tr.triggerOn)

        // 替换yaml模板
        val yamlReplaceResult = prepareCIBuildYaml(action) ?: return false
        val yamlObject = yamlReplaceResult.normalYaml
        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        action.data.context.parsedYaml = parsedYaml
        action.data.context.normalizedYaml = normalizedYaml
        logger.info(
            "StreamYamlTrigger|triggerBuild|pipelineId|${pipeline.pipelineId}" +
                "|parsedYaml|$parsedYaml|normalize yaml|$normalizedYaml"
        )

        if (action is TGitNoteActionGit || action is TGitReviewActionGit) {
            HandsomeUtil.issue8910(normalizedYaml, pipeline.gitProjectId, pipeline.pipelineId, pipeline.filePath)
        }
        // 除了新建的流水线，若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称，只在当前yml文件变更时进行
        if (needChangePipelineDisplayName(action)) {
            pipeline.displayName = yamlObject.name?.ifBlank {
                pipeline.filePath
            } ?: pipeline.filePath
        }

        streamBasicSettingService.updateProjectInfo(
            action.data.getUserId(),
            gitProjectInfo.toStreamGitProjectInfoWithProject()
        )

        if (isTiming) {
            // 定时注册事件
            logger.warn(
                "StreamYamlTrigger|triggerBuild|special job register|timer|$isTiming" +
                    "|gitProjectId|${action.data.getGitProjectId()}|eventId|${action.data.context.requestEventId!!}"
            )
            yamlBuild.gitStartBuild(
                action = action,
                triggerResult = tr,
                startParams = startParams,
                yaml = yamlObject,
                gitBuildId = null,
                // 没有触发只有特殊任务的需要保存一下蓝盾流水线
                onlySavePipeline = !isTrigger,
                yamlTransferData = yamlReplaceResult.yamlTransferData,
                manualInputs = null
            )
        }

        if (isDelete || !repoHookName.isNullOrEmpty()) {
            // 有特殊任务的注册事件
            logger.warn(
                "StreamYamlTrigger|triggerBuild|special job register|delete|$isDelete|repoHookName|$repoHookName" +
                    "|gitProjectId|${action.data.getGitProjectId()}" +
                    "|eventId|${action.data.context.requestEventId!!}"
            )
            yamlBuild.gitStartBuild(
                action = action,
                triggerResult = tr,
                startParams = startParams,
                yaml = yamlObject,
                gitBuildId = null,
                // 没有触发只有特殊任务的不需要保存蓝盾流水线
                onlySavePipeline = false,
                yamlTransferData = yamlReplaceResult.yamlTransferData,
                manualInputs = null
            )
        }

        if (isTrigger) {
            // 正常匹配仓库操作触发
            logger.info(
                "StreamYamlTrigger|triggerBuild|Matcher|gitProjectId|${action.data.getGitProjectId()}|" +
                    "eventId|${action.data.context.requestEventId!!}|pipeline|$pipeline"
            )

            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = action.data.context.requestEventId!!,
                originYaml = action.data.context.originYaml!!,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = action.data.getGitProjectId().toLong(),
                branch = action.data.eventCommon.branch,
                objectKind = action.metaData.streamObjectKind.value,
                commitMsg = action.data.eventCommon.commit.commitMsg,
                triggerUser = action.data.eventCommon.userId,
                sourceGitProjectId = action.data.eventCommon.sourceGitProjectId?.toLong(),
                buildStatus = BuildStatus.RUNNING,
                version = ymlVersion
            )
            yamlBuild.gitStartBuild(
                action = action,
                triggerResult = tr,
                startParams = startParams,
                yaml = yamlObject,
                gitBuildId = gitBuildId,
                onlySavePipeline = false,
                yamlTransferData = yamlReplaceResult.yamlTransferData,
                manualInputs = null
            )
        }
        return true
    }

    private fun getDisplayName(action: BaseAction): String {
        val originYaml = action.data.context.originYaml!!
        val ymlName = ScriptYmlUtils.parseName(originYaml)?.name
        val pipeline = action.data.context.pipeline!!
        return if (!ymlName.isNullOrBlank()) {
            ymlName
        } else {
            pipeline.filePath
        }
    }

    private fun needUpdateLastBuildBranch(action: BaseAction): Boolean {
        return action.data.context.pipeline!!.pipelineId.isBlank() ||
            (
                action is GitBaseAction &&
                    !action.getChangeSet().isNullOrEmpty() &&
                    action.getChangeSet()!!.toSet()
                        .contains(action.data.context.pipeline!!.filePath)
                )
    }

    @Throws(StreamTriggerBaseException::class, ErrorCodeException::class)
    fun prepareCIBuildYaml(
        action: BaseAction
    ): YamlReplaceResult? {
        logger.info(
            "StreamYamlTrigger|prepareCIBuildYaml" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )

        if (action.data.context.originYaml.isNullOrBlank()) {
            return null
        }

        val preTemplateYamlObject = YamlFormat.formatYaml(action)

        // 保存引用的pool信息
        val resourcePoolExt = mutableMapOf<String, ResourcesPools>()
        preTemplateYamlObject.resources?.pools?.forEach { pool ->
            resourcePoolExt[pool.format()] = pool
        }

        val filePath = action.data.context.pipeline!!.filePath.ifBlank { STREAM_TEMPLATE_ROOT_FILE }
        // 替换yaml文件中的模板引用
        try {
            val preYamlObject = YamlTemplate(
                yamlObject = preTemplateYamlObject,
                filePath = filePath,
                extraParameters = action,
                getTemplateMethod = yamlTemplateService::getTemplate,
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = resourcePoolExt,
                conf = YamlTemplateConf(
                    useOldParametersExpression = action.data.context.pipelineAsCodeSettings?.enable != true
                )
            ).replace()

            val newPreYamlObject = preYamlObject.copy(
                resources = Resources(
                    repositories = preYamlObject.resources?.repositories,
                    pools = resourcePoolExt.values.toList()
                )
            )

            val (normalYaml, transferData) = ScriptYmlUtils.normalizeGitCiYaml(newPreYamlObject, filePath)
            return YamlReplaceResult(
                preYaml = newPreYamlObject,
                normalYaml = normalYaml,
                yamlTransferData = if (transferData.templateData.transferDataMap.isEmpty()) {
                    null
                } else {
                    transferData
                }
            )
        } catch (e: Throwable) {
            logger.info(
                "StreamYamlTrigger|prepareCIBuildYaml" +
                    "|event|${action.data.context.requestEventId}|error",
                e
            )
            val isMr = action.metaData.isStreamMr()
            val (block, message, reason) = when (e) {
                is YamlBlankException -> {
                    Triple(isMr, "${e.repo} ${e.filePath} is null", TriggerReason.CI_YAML_CONTENT_NULL)
                }
                is YamlBehindException -> {
                    Triple(isMr, e.filePath, TriggerReason.CI_YAML_NEED_MERGE_OR_REBASE)
                }
                is YamlFormatException, is JsonProcessingException, is CustomException, is TypeCastException -> {
                    Triple(isMr, e.message, TriggerReason.CI_YAML_TEMPLATE_ERROR)
                }
                is StackOverflowError -> {
                    Triple(isMr, "Yaml file has circular dependency", TriggerReason.CI_YAML_TEMPLATE_ERROR)
                }
                // 指定异常直接扔出在外面统一处理
                is StreamTriggerBaseException, is ErrorCodeException -> {
                    throw e
                }
                else -> {
                    logger.warn("StreamYamlTrigger|prepareCIBuildYaml|${action.data.context.requestEventId}|error", e)
                    Triple(false, e.message, TriggerReason.UNKNOWN_ERROR)
                }
            }
            throw StreamTriggerException(
                action = action,
                triggerReason = reason,
                reasonParams = listOf(message ?: ""),
                commitCheck = CommitCheck(
                    block = block,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }
    }

    private fun needChangePipelineDisplayName(
        action: BaseAction
    ): Boolean {
        return action is GitBaseAction && action.data.context.pipeline!!.displayName != getDisplayName(action)
    }
}
