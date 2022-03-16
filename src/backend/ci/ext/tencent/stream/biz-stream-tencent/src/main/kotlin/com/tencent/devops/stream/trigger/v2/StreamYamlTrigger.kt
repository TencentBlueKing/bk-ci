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

package com.tencent.devops.stream.trigger.v2

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.exception.YamlFormatException
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException.Companion.triggerError
import com.tencent.devops.stream.common.exception.YamlBlankException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.stream.trigger.YamlTriggerInterface
import com.tencent.devops.stream.v2.service.StreamScmService
import com.tencent.devops.common.ci.v2.parsers.template.YamlTemplate
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import com.tencent.devops.stream.common.exception.YamlBehindException
import com.tencent.devops.stream.config.StreamStorageBean
import com.tencent.devops.stream.pojo.isFork
import com.tencent.devops.stream.trigger.pojo.StreamTriggerContext
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlFormat
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.pojo.YamlReplaceResult
import com.tencent.devops.stream.trigger.template.TemplateProjectData
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class StreamYamlTrigger @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamScmService: StreamScmService,
    private val streamGitTokenService: StreamGitTokenService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitBasicSettingService: StreamBasicSettingService,
    private val yamlTemplateService: YamlTemplateService,
    private val triggerMatcher: TriggerMatcher,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val yamlBuildV2: StreamYamlBuild,
    private val tokenService: StreamGitTokenService,
    private val streamStorageBean: StreamStorageBean
) : YamlTriggerInterface {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlTrigger::class.java)
        const val ymlVersion = "v2.0"

        // 针对filePath可能为空的情况下创建一个模板替换的根目录名称
        private const val STREAM_TEMPLATE_ROOT_FILE = "STREAM_TEMPLATE_ROOT_FILE"
    }

    override fun triggerBuild(
        context: StreamTriggerContext
    ): Boolean {
        val start = LocalDateTime.now().timestampmilli()

        // TODO: 暂时先全部展开，后续函数全替换为上下文参数即可去掉
        val (event, gitRequestEvent, _, gitProjectPipeline, originYaml, mrChangeSet) = context

        val gitProjectInfo = streamScmService.getProjectInfoRetry(
            token = streamGitTokenService.getToken(gitRequestEvent.gitProjectId),
            gitProjectId = gitRequestEvent.gitProjectId.toString(),
            useAccessToken = true
        )

        val (isTrigger, isTiming, startParams, isDelete) = triggerMatcher.isMatch(
            context = context,
            defaultBranch = gitProjectInfo.defaultBranch
        )

        val changeSet = triggerMatcher.getChangeSet(context)

        if (!isTrigger && !isTiming && !isDelete) {
            logger.warn(
                "${gitProjectPipeline.pipelineId}|" +
                        "Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                        "eventId: ${gitRequestEvent.id}"
            )
            triggerError(
                request = gitRequestEvent,
                event = event,
                pipeline = gitProjectPipeline,
                reason = TriggerReason.TRIGGER_NOT_MATCH,
                yamls = Yamls(originYaml, null, null),
                version = ymlVersion
            )
        }

        val yamlReplaceResult = prepareCIBuildYaml(
            gitRequestEvent = gitRequestEvent,
            isMr = (event is GitMergeRequestEvent),
            originYaml = originYaml,
            filePath = gitProjectPipeline.filePath,
            pipelineId = gitProjectPipeline.pipelineId,
            pipelineName = gitProjectPipeline.displayName,
            event = event,
            changeSet = mrChangeSet,
            forkGitProjectId = gitRequestEvent.getForkGitProjectId()
        ) ?: return false
        val yamlObject = yamlReplaceResult.normalYaml
        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlReplaceResult.preYaml)
        logger.info("${gitProjectPipeline.pipelineId} parsedYaml: $parsedYaml normalize yaml: $normalizedYaml")

        // 除了新建的流水线，若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称，只在当前yml文件变更时进行
        if (gitProjectPipeline.pipelineId.isBlank() ||
            (!changeSet.isNullOrEmpty() && changeSet.contains(gitProjectPipeline.filePath))
        ) {
            gitProjectPipeline.displayName = yamlObject.name?.ifBlank {
                gitProjectPipeline.filePath
            } ?: gitProjectPipeline.filePath
        }

        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态，只有url或者名称不对才更新
        gitBasicSettingService.updateProjectInfo(gitRequestEvent.userId, gitProjectInfo)

        if (isTiming || isDelete) {
            // 有特殊任务的注册事件
            logger.warn(
                "special job register timer: $isTiming delete: $isDelete" +
                        "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
            )
            yamlBuildV2.gitStartBuild(
                pipeline = gitProjectPipeline,
                event = gitRequestEvent,
                yaml = yamlObject,
                parsedYaml = parsedYaml,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitBuildId = null,
                changeSet = changeSet,
                // 没有触发只有定时任务的需要保存一下蓝盾流水线
                isTimeTrigger = isTiming,
                isDeleteTrigger = isDelete,
                // 没有触发只有特殊任务的需要保存一下蓝盾流水线
                onlySavePipeline = !isTrigger,
                gitProjectInfo = gitProjectInfo,
                yamlTransferData = yamlReplaceResult.yamlTransferData
            )
        }

        streamStorageBean.prepareYamlTime(LocalDateTime.now().timestampmilli() - start)

        if (isTrigger) {
            // 正常匹配仓库操作触发
            logger.info(
                "Matcher is true, display the event, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                        "eventId: ${gitRequestEvent.id}, dispatched pipeline: $gitProjectPipeline"
            )
            // TODO：后续将这个先存储再修改的操作全部重构，打平过度设计的抽象BaseBuild类，将构建分为准备段和构建段
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = if (context.gitEvent.isDeleteEvent()) {
                    TGitObjectKind.OBJECT_KIND_DELETE
                } else gitRequestEvent.objectKind,
                commitMsg = gitRequestEvent.commitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
                buildStatus = BuildStatus.RUNNING,
                version = ymlVersion
            )
            yamlBuildV2.gitStartBuild(
                pipeline = gitProjectPipeline,
                event = gitRequestEvent,
                yaml = yamlObject,
                parsedYaml = parsedYaml,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitBuildId = gitBuildId,
                isTimeTrigger = false,
                onlySavePipeline = false,
                changeSet = changeSet,
                params = startParams,
                yamlTransferData = yamlReplaceResult.yamlTransferData
            )
        }
        return true
    }

    @Throws(TriggerBaseException::class, ErrorCodeException::class)
    fun prepareCIBuildYaml(
        gitRequestEvent: GitRequestEvent,
        isMr: Boolean,
        originYaml: String?,
        filePath: String,
        pipelineId: String?,
        pipelineName: String?,
        event: GitEvent?,
        changeSet: Set<String>?,
        forkGitProjectId: Long?
    ): YamlReplaceResult? {
        if (originYaml.isNullOrBlank()) {
            return null
        }
        logger.info("input yamlStr: $originYaml")
        val preTemplateYamlObject = YamlFormat.formatYaml(
            originYaml = originYaml,
            gitRequestEvent = gitRequestEvent,
            filePath = filePath,
            isMr = isMr
        )
        return replaceYamlTemplate(
            isFork = gitRequestEvent.isFork(),
            isMr = isMr,
            forkGitProjectId = forkGitProjectId,
            preTemplateYamlObject = preTemplateYamlObject,
            filePath = filePath.ifBlank { STREAM_TEMPLATE_ROOT_FILE },
            gitRequestEvent = gitRequestEvent,
            originYaml = originYaml,
            event = event,
            changeSet = changeSet
        )
    }

    override fun checkYamlSchema(userId: String, yaml: String): Result<String> {
        return try {
            yamlSchemaCheck.check(yaml, null, true)
            Result("OK")
        } catch (e: Exception) {
            logger.error("Check yaml schema failed.", e)
            Result(1, "Invalid yaml: ${e.message}")
        }
    }

    @Throws(TriggerBaseException::class, ErrorCodeException::class)
    private fun replaceYamlTemplate(
        isFork: Boolean,
        isMr: Boolean,
        preTemplateYamlObject: PreTemplateScriptBuildYaml,
        filePath: String,
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        event: GitEvent?,
        changeSet: Set<String>?,
        forkGitProjectId: Long?
    ): YamlReplaceResult {
        // 替换yaml文件中的模板引用
        try {
            val preYamlObject = YamlTemplate(
                yamlObject = preTemplateYamlObject,
                filePath = filePath,
                extraParameters = TemplateProjectData(
                    gitRequestEventId = gitRequestEvent.id!!,
                    triggerProjectId = streamScmService.getProjectId(isFork, gitRequestEvent),
                    triggerUserId = gitRequestEvent.userId,
                    sourceProjectId = gitRequestEvent.gitProjectId,
                    triggerRef = gitRequestEvent.branch,
                    triggerToken = tokenService.getToken(gitRequestEvent.gitProjectId),
                    forkGitToken = if (forkGitProjectId != null) {
                        tokenService.getToken(forkGitProjectId)
                    } else null,
                    changeSet = changeSet,
                    event = event
                ),
                getTemplateMethod = yamlTemplateService::getTemplate,
                nowRepo = null,
                repo = null
            ).replace()
            val (normalYaml, transferData) = ScriptYmlUtils.normalizeGitCiYaml(preYamlObject, filePath)
            return YamlReplaceResult(
                preYaml = preYamlObject, normalYaml = normalYaml, yamlTransferData = transferData
            )
        } catch (e: Throwable) {
            logger.info("event ${gitRequestEvent.id} yaml template replace error", e)
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
                is TriggerBaseException, is ErrorCodeException -> {
                    throw e
                }
                else -> {
                    logger.error("event: ${gitRequestEvent.id} unknow error: ${e.message}")
                    Triple(false, e.message, TriggerReason.UNKNOWN_ERROR)
                }
            }
            triggerError(
                request = gitRequestEvent,
                filePath = filePath,
                reason = reason,
                reasonParams = listOf(message ?: ""),
                yamls = Yamls(originYaml, null, null),
                version = ymlVersion,
                commitCheck = CommitCheck(
                    block = block,
                    state = GitCICommitCheckState.FAILURE
                )
            )
        }
    }
}
