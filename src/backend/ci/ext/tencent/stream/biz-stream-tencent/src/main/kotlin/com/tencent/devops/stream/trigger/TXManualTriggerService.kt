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
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamCode.BK_STARTUP_CONFIGURATION_MISSING
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.V1TriggerBuildReq
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamPipelineService
import com.tencent.devops.stream.service.StreamYamlService
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import com.tencent.devops.stream.v1.components.V1GitRequestEventHandle
import com.tencent.devops.stream.v1.components.V1YamlBuild
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import com.tencent.devops.stream.v1.service.V1GitCIEventService
import com.tencent.devops.stream.v1.service.V1StreamYamlService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Primary
@Service
@SuppressWarnings("LongParameterList")
class TXManualTriggerService @Autowired constructor(
    actionFactory: EventActionFactory,
    private val streamGitConfig: StreamGitConfig,
    streamEventService: StreamEventService,
    streamBasicSettingService: StreamBasicSettingService,
    streamYamlTrigger: StreamYamlTrigger,
    streamBasicSettingDao: StreamBasicSettingDao,
    streamYamlBuild: StreamYamlBuild,
    yamlTemplateService: YamlTemplateService,
    private val dslContext: DSLContext,
    private val client: Client,
    private val gitRequestEventHandle: V1GitRequestEventHandle,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitCIEventService: V1GitCIEventService,
    private val yamlBuild: V1YamlBuild,
    private val streamYamlService: V1StreamYamlService,
    private val streamPipelineService: StreamPipelineService,
    private val streamYamlServiceV2: StreamYamlService
) : ManualTriggerService(
    dslContext = dslContext,
    client = client,
    actionFactory = actionFactory,
    streamGitConfig = streamGitConfig,
    streamEventService = streamEventService,
    streamBasicSettingService = streamBasicSettingService,
    streamYamlTrigger = streamYamlTrigger,
    streamBasicSettingDao = streamBasicSettingDao,
    gitRequestEventDao = gitRequestEventDao,
    gitPipelineResourceDao = gitPipelineResourceDao,
    gitRequestEventBuildDao = gitRequestEventBuildDao,
    streamYamlBuild = streamYamlBuild,
    yamlTemplateService = yamlTemplateService,
    streamPipelineService = streamPipelineService,
    streamYamlService = streamYamlServiceV2
) {

    @Value("\${rtx.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(TXManualTriggerService::class.java)
    }

    override fun triggerBuild(
        userId: String,
        pipelineId: String,
        triggerBuildReq: TriggerBuildReq
    ): TriggerBuildResult {
        return triggerBuild(
            userId = userId,
            pipelineId = pipelineId,
            v1TriggerBuildReq = V1TriggerBuildReq(
                gitProjectId = GitCommonUtils.getGitProjectId(triggerBuildReq.projectId),
                name = null,
                url = null,
                homepage = null,
                gitHttpUrl = null,
                gitSshUrl = null,
                branch = triggerBuildReq.branch,
                customCommitMsg = triggerBuildReq.customCommitMsg,
                yaml = triggerBuildReq.yaml,
                description = triggerBuildReq.description,
                commitId = triggerBuildReq.commitId
            ),
            inputs = triggerBuildReq.inputs
        )
    }

    /**
     * 内部版本的因为有V1语法，所以和openapi触发拆开，openapi触发直接走core版
     */
    fun triggerBuild(
        userId: String,
        pipelineId: String,
        v1TriggerBuildReq: V1TriggerBuildReq,
        inputs: Map<String, String>?
    ): TriggerBuildResult {
        logger.info(
            "TXManualTriggerService|triggerBuild" +
                "|userId|$userId|pipeline|$pipelineId|v1TriggerBuildReq|$v1TriggerBuildReq"
        )

        val originYaml = v1TriggerBuildReq.yaml
        // 如果当前文件没有内容直接不触发
        if (originYaml.isNullOrBlank()) {
            throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.CI_YAML_CONTENT_NULL.name +
                    "(${TriggerReason.CI_YAML_CONTENT_NULL.detail.format("")})"
            )
        }

        // v1走老逻辑手动触发，v2走core版逻辑
        return if (!ScriptYmlUtils.isV2Version(originYaml)) {
            v1trigger(
                userId = userId,
                pipelineId = pipelineId,
                v1TriggerBuildReq = v1TriggerBuildReq
            )
        } else {
            super.triggerBuild(
                userId = userId,
                pipelineId = pipelineId,
                triggerBuildReq = TriggerBuildReq(
                    projectId = GitCommonUtils.getCiProjectId(
                        v1TriggerBuildReq.gitProjectId,
                        streamGitConfig.getScmType()
                    ),
                    branch = v1TriggerBuildReq.branch,
                    customCommitMsg = v1TriggerBuildReq.customCommitMsg,
                    yaml = v1TriggerBuildReq.yaml,
                    description = v1TriggerBuildReq.description,
                    commitId = v1TriggerBuildReq.commitId,
                    payload = v1TriggerBuildReq.payload,
                    eventType = v1TriggerBuildReq.eventType,
                    inputs = inputs
                )
            )
        }
    }

    private fun v1trigger(
        userId: String,
        pipelineId: String,
        v1TriggerBuildReq: V1TriggerBuildReq
    ): TriggerBuildResult {
        // open api模拟工蜂事件触发
        val gitRequestEvent = gitRequestEventHandle.createManualTriggerEvent(userId, v1TriggerBuildReq)

        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

        val gitRequestEventForHandle = manualChangeGitRequestEvent(gitRequestEvent)

        val existsPipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, v1TriggerBuildReq.gitProjectId, pipelineId)
                ?: throw OperationException("stream pipeline: $pipelineId is not exist")
        // 如果该流水线已保存过，则继续使用
        val buildPipeline = StreamGitProjectPipeline(
            gitProjectId = existsPipeline.gitProjectId,
            pipelineId = existsPipeline.pipelineId,
            filePath = existsPipeline.filePath,
            displayName = existsPipeline.displayName,
            enabled = existsPipeline.enabled,
            creator = existsPipeline.creator,
            latestBuildBranch = null
        )

        // 流水线未启用在手动触发处直接报错
        if (!buildPipeline.enabled) {
            throw CustomException(
                status = Response.Status.METHOD_NOT_ALLOWED,
                message = "${TriggerReason.PIPELINE_DISABLE.name}(${TriggerReason.PIPELINE_DISABLE.detail})"
            )
        }

        val originYaml = v1TriggerBuildReq.yaml!!

        val (yamlObject, normalizedYaml) =
            prepareCIBuildYaml(
                gitRequestEvent = gitRequestEventForHandle.gitRequestEvent,
                originYaml = originYaml,
                filePath = existsPipeline.filePath,
                pipelineId = existsPipeline.pipelineId,
                pipelineName = existsPipeline.displayName
            ) ?: throw CustomException(
                status = Response.Status.BAD_REQUEST,
                message = TriggerReason.CI_YAML_INVALID.name +
                    "(${TriggerReason.CI_YAML_INVALID.detail.format("prepare error")})"
            )

        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = gitRequestEventForHandle.id!!,
            originYaml = originYaml,
            parsedYaml = originYaml,
            normalizedYaml = normalizedYaml,
            gitProjectId = gitRequestEventForHandle.gitProjectId,
            branch = gitRequestEventForHandle.branch,
            objectKind = gitRequestEventForHandle.gitRequestEvent.objectKind,
            commitMsg = v1TriggerBuildReq.customCommitMsg,
            triggerUser = gitRequestEventForHandle.userId,
            sourceGitProjectId = gitRequestEventForHandle.gitRequestEvent.sourceGitProjectId,
            buildStatus = BuildStatus.RUNNING,
            version = null
        )
        val result = yamlBuild.gitStartBuild(
            pipeline = StreamTriggerPipeline(
                gitProjectId = buildPipeline.gitProjectId.toString(),
                pipelineId = buildPipeline.pipelineId,
                filePath = buildPipeline.filePath,
                displayName = buildPipeline.displayName,
                enabled = buildPipeline.enabled,
                creator = buildPipeline.creator
            ),
            event = V1GitRequestEvent(
                id = gitRequestEvent.id,
                objectKind = gitRequestEvent.objectKind,
                operationKind = gitRequestEvent.operationKind,
                extensionAction = gitRequestEvent.extensionAction,
                gitProjectId = gitRequestEvent.gitProjectId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
                branch = gitRequestEvent.branch,
                targetBranch = gitRequestEvent.targetBranch,
                commitId = gitRequestEvent.commitId,
                commitMsg = gitRequestEvent.commitMsg,
                commitTimeStamp = gitRequestEvent.commitTimeStamp,
                commitAuthorName = gitRequestEvent.commitAuthorName,
                userId = gitRequestEvent.userId,
                totalCommitCount = gitRequestEvent.totalCommitCount,
                mergeRequestId = gitRequestEvent.mergeRequestId,
                event = gitRequestEvent.event,
                description = gitRequestEvent.description,
                mrTitle = gitRequestEvent.mrTitle,
                gitEvent = gitRequestEvent.gitEvent,
                gitProjectName = gitRequestEvent.gitProjectName
            ),
            yaml = yamlObject,
            gitBuildId = gitBuildId
        ) ?: throw CustomException(
            status = Response.Status.BAD_REQUEST,
            message = TriggerReason.PIPELINE_RUN_ERROR.name +
                "(${TriggerReason.PIPELINE_RUN_ERROR.detail})"
        )
        return TriggerBuildResult(
            projectId = GitCommonUtils.getCiProjectId(v1TriggerBuildReq.gitProjectId, streamGitConfig.getScmType()),
            branch = v1TriggerBuildReq.branch,
            customCommitMsg = v1TriggerBuildReq.customCommitMsg,
            description = v1TriggerBuildReq.description,
            commitId = v1TriggerBuildReq.commitId,
            buildId = result.id,
            buildUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
                homePage = v2GitUrl ?: throw ParamBlankException(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_STARTUP_CONFIGURATION_MISSING,
                        language = I18nUtil.getLanguage(userId)
                    )
                ),
                gitProjectId = buildPipeline.gitProjectId,
                pipelineId = pipelineId,
                buildId = result.id
            )
        )
    }

    private fun manualChangeGitRequestEvent(
        gitRequestEvent: GitRequestEvent
    ): GitRequestEventForHandle {
        return GitRequestEventForHandle(
            id = gitRequestEvent.id,
            gitProjectId = gitRequestEvent.gitProjectId,
            branch = gitRequestEvent.branch,
            userId = gitRequestEvent.userId,
            checkRepoTrigger = false,
            gitRequestEvent = gitRequestEvent
        )
    }

    private fun prepareCIBuildYaml(
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        filePath: String,
        pipelineId: String?,
        pipelineName: String?
    ): Pair<CIBuildYaml, String>? {
        if (originYaml.isNullOrBlank()) {
            return null
        }

        val yamlObject = try {
            streamYamlService.createCIBuildYaml(originYaml, gitRequestEvent.gitProjectId)
        } catch (e: Throwable) {
            logger.warn("TXManualTriggerService|prepareCIBuildYaml|v1 git ci yaml is invalid", e)
            // 手动触发不发送commitCheck
            gitCIEventService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_INVALID.name,
                reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message),
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = null,
                branch = gitRequestEvent.branch
            )
            return null
        }

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("TXManualTriggerService|prepareCIBuildYaml|normalize yaml|$normalizedYaml")
        return Pair(yamlObject, normalizedYaml)
    }
}
