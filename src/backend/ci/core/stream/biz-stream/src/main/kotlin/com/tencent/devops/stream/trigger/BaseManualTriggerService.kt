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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.yaml.v2.enums.needInput
import com.tencent.devops.process.yaml.v2.models.on.EnableType
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.STARTUP_CONFIG_MISSING
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.StreamManualAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamOpenApiAction
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandlerUtil
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.toStreamGitProjectInfoWithProject
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.ManualPreScriptBuildYaml
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@SuppressWarnings("LongParameterList", "LongMethod", "ThrowsCount")
abstract class BaseManualTriggerService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamGitConfig: StreamGitConfig,
    private val streamEventService: StreamEventService,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val streamYamlTrigger: StreamYamlTrigger,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamYamlBuild: StreamYamlBuild
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BaseManualTriggerService::class.java)
    }

    open fun triggerBuild(userId: String, pipelineId: String, triggerBuildReq: TriggerBuildReq): TriggerBuildResult {
        logger.info(
            "BaseManualTriggerService|triggerBuild" +
                "|userId|$userId|pipeline|$pipelineId|triggerBuildReq|$triggerBuildReq"
        )

        val streamTriggerSetting = getSetting(triggerBuildReq)

        // open api 通过提供事件原文模拟事件触发
        val action = loadAction(
            streamTriggerSetting = streamTriggerSetting,
            userId = userId,
            triggerBuildReq = triggerBuildReq
        )

        val buildPipeline = gitPipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = action.data.getGitProjectId().toLong(),
            pipelineId = pipelineId
        )?.let {
            StreamTriggerPipeline(
                gitProjectId = it.gitProjectId.toString(),
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator
            )
        } ?: throw OperationException("stream pipeline: $pipelineId is not exist")

        // 流水线未启用在手动触发处直接报错
        if (!buildPipeline.enabled) {
            throw CustomException(
                status = Response.Status.METHOD_NOT_ALLOWED,
                message = "${TriggerReason.PIPELINE_DISABLE.name}(${TriggerReason.PIPELINE_DISABLE.detail})"
            )
        }

        action.data.context.pipeline = buildPipeline

        // 获取蓝盾流水线的pipelineAsCodeSetting
        val projectCode =
            GitCommonUtils.getCiProjectId(buildPipeline.gitProjectId.toLong(), streamGitConfig.getScmType())
        action.data.context.pipelineAsCodeSettings = try {
            if (buildPipeline.pipelineId.isNotBlank()) {
                client.get(ServicePipelineSettingResource::class).getPipelineSetting(
                    projectId = projectCode,
                    pipelineId = buildPipeline.pipelineId,
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

        val originYaml = triggerBuildReq.yaml
        // 如果当前文件没有内容直接不触发
        if (originYaml.isNullOrBlank()) {
            logger.warn(
                "BaseManualTriggerService|triggerBuild" +
                    "|yaml is null|event|${action.data.context.requestEventId}"
            )
            streamEventService.saveBuildNotBuildEvent(
                action = action,
                reason = TriggerReason.CI_YAML_CONTENT_NULL.name,
                reasonDetail = TriggerReason.CI_YAML_CONTENT_NULL.detail,
                sendCommitCheck = action.needSendCommitCheck(),
                commitCheckBlock = false,
                version = "v2.0"
            )
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.CI_YAML_CONTENT_NULL.name +
                    "(${TriggerReason.CI_YAML_CONTENT_NULL.detail.format("")})"
            )
        }

        action.data.context.originYaml = originYaml

        val result = handleTrigger(
            action = action,
            originYaml = originYaml,
            triggerBuildReq = triggerBuildReq
        ) ?: throw CustomException(
            status = Response.Status.BAD_REQUEST,
            message = TriggerReason.PIPELINE_RUN_ERROR.name +
                "(${TriggerReason.PIPELINE_RUN_ERROR.detail})"
        )
        return TriggerBuildResult(
            projectId = GitCommonUtils.getCiProjectId(
                action.data.eventCommon.gitProjectId.toLong(),
                streamGitConfig.getScmType()
            ),
            branch = triggerBuildReq.branch,
            customCommitMsg = triggerBuildReq.customCommitMsg,
            description = triggerBuildReq.description,
            commitId = triggerBuildReq.commitId,
            buildId = result.id,
            buildUrl = StreamPipelineUtils.genStreamV2BuildUrl(
                homePage = streamGitConfig.streamUrl ?: throw ParamBlankException(
                    MessageUtil.getMessageByLocale(
                        STARTUP_CONFIG_MISSING,
                        I18nUtil.getLanguage(userId),
                        arrayOf(" streamUrl")
                    )
                ),
                gitProjectId = buildPipeline.gitProjectId,
                pipelineId = pipelineId,
                buildId = result.id
            )
        )
    }

    protected fun getSetting(triggerBuildReq: TriggerBuildReq): StreamTriggerSetting {
        val streamTriggerSetting = streamBasicSettingDao.getSetting(
            dslContext = dslContext,
            gitProjectId = GitCommonUtils.getGitProjectId(triggerBuildReq.projectId),
            hasLastInfo = false
        )?.let {
            StreamTriggerSetting(
                enableCi = it.enableCi,
                buildPushedBranches = it.buildPushedBranches,
                buildPushedPullRequest = it.buildPushedPullRequest,
                enableUser = it.enableUserId,
                gitHttpUrl = it.gitHttpUrl,
                projectCode = it.projectCode,
                enableCommitCheck = it.enableCommitCheck,
                enableMrBlock = it.enableMrBlock,
                name = it.name,
                enableMrComment = it.enableMrComment,
                homepage = it.homepage,
                triggerReviewSetting = it.triggerReviewSetting
            )
        } ?: throw CustomException(Response.Status.FORBIDDEN, message = TriggerReason.CI_DISABLED.detail)
        return streamTriggerSetting
    }

    abstract fun loadAction(
        streamTriggerSetting: StreamTriggerSetting,
        userId: String,
        triggerBuildReq: TriggerBuildReq
    ): BaseAction

    abstract fun getStartParams(action: BaseAction, triggerBuildReq: TriggerBuildReq): Map<String, String>

    abstract fun getInputParams(action: BaseAction, triggerBuildReq: TriggerBuildReq): Map<String, String>?

    fun handleTrigger(
        action: BaseAction,
        originYaml: String,
        triggerBuildReq: TriggerBuildReq
    ): BuildId? {
        logger.info(
            "BaseManualTriggerService|handleTrigger" +
                "|requestEventId|${action.data.context.requestEventId}|action|${action.format()}"
        )

        var buildId: BuildId? = null
        StreamTriggerExceptionHandlerUtil.handleManualTrigger {
            buildId = trigger(action, originYaml, triggerBuildReq)
        }
        return buildId
    }

    @BkTimed
    private fun trigger(
        action: BaseAction,
        originYaml: String,
        triggerBuildReq: TriggerBuildReq
    ): BuildId? {
        val yamlReplaceResult = streamYamlTrigger.prepareCIBuildYaml(action)!!
        val parsedYaml = if (action.metaData.streamObjectKind.needInput()) {
            YamlCommonUtils.toYamlNotNull(
                ManualPreScriptBuildYaml(yamlReplaceResult.preYaml, getInputParams(action, triggerBuildReq))
            )
        } else {
            YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        }
        val normalizedYaml = YamlUtil.toYaml(yamlReplaceResult.normalYaml)

        val triggerOn = yamlReplaceResult.preYaml.triggerOn
        if ((action is StreamManualAction) && (triggerOn?.manual == EnableType.FALSE.value)) {
            throw CustomException(Response.Status.BAD_REQUEST, "manual trigger is disabled")
        }

        if ((action is StreamOpenApiAction) && (triggerOn?.openapi == EnableType.FALSE.value)) {
            throw CustomException(Response.Status.BAD_REQUEST, "openapi trigger is disabled")
        }

        action.data.context.parsedYaml = parsedYaml
        action.data.context.normalizedYaml = normalizedYaml

        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        val gitProjectInfo = action.api.getGitProjectInfo(
            action.getGitCred(),
            action.getGitProjectIdOrName(),
            ApiRequestRetryInfo(true)
        )!!.toStreamGitProjectInfoWithProject()
        streamBasicSettingService.updateProjectInfo(action.data.getUserId(), gitProjectInfo)
        action.data.setting = action.data.setting.copy(gitHttpUrl = gitProjectInfo.gitHttpUrl)

        val params = getStartParams(action = action, triggerBuildReq = triggerBuildReq)

        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = action.data.context.requestEventId!!,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = normalizedYaml,
            gitProjectId = action.data.getGitProjectId().toLong(),
            branch = action.data.eventCommon.branch,
            objectKind = action.metaData.streamObjectKind.value,
            commitMsg = triggerBuildReq.customCommitMsg,
            triggerUser = action.data.eventCommon.userId,
            sourceGitProjectId = action.data.eventCommon.sourceGitProjectId?.toLong(),
            buildStatus = BuildStatus.RUNNING,
            version = "v2.0"
        )
        return streamYamlBuild.gitStartBuild(
            action = action,
            triggerResult = TriggerResult(
                trigger = TriggerBody(true),
                triggerOn = null,
                timeTrigger = false,
                deleteTrigger = false
            ),
            startParams = params,
            yaml = yamlReplaceResult.normalYaml,
            gitBuildId = gitBuildId,
            onlySavePipeline = false,
            yamlTransferData = yamlReplaceResult.yamlTransferData,
            manualInputs = getInputParams(action, triggerBuildReq)
        )
    }
}
