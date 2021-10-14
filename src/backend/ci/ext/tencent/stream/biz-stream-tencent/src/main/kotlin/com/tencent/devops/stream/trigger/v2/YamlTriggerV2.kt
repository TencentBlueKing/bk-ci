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
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.exception.YamlFormatException
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException.Companion.triggerError
import com.tencent.devops.stream.common.exception.YamlBlankException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.git.GitEvent
import com.tencent.devops.stream.pojo.git.GitMergeRequestEvent
import com.tencent.devops.stream.pojo.v2.YamlObjects
import com.tencent.devops.stream.trigger.YamlTriggerInterface
import com.tencent.devops.stream.v2.service.ScmService
import com.tencent.devops.stream.trigger.template.YamlTemplate
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.trigger.template.pojo.TemplateGraph
import com.tencent.devops.stream.v2.service.GitCIBasicSettingService
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.stream.common.exception.YamlBehindException
import com.tencent.devops.stream.trigger.parsers.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.YamlCheck
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class YamlTriggerV2 @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmService: ScmService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitBasicSettingService: GitCIBasicSettingService,
    private val yamlTemplateService: YamlTemplateService,
    private val triggerMatcher: TriggerMatcher,
    private val yamlCheck: YamlCheck,
    private val yamlBuildV2: YamlBuildV2
) : YamlTriggerInterface<YamlObjects> {

    companion object {
        private val logger = LoggerFactory.getLogger(YamlTriggerV2::class.java)
        const val ymlVersion = "v2.0"

        // 针对filePath可能为空的情况下创建一个模板替换的根目录名称
        private const val STREAM_TEMPLATE_ROOT_FILE = "STREAM_TEMPLATE_ROOT_FILE"
    }

    override fun triggerBuild(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        gitProjectPipeline: GitProjectPipeline,
        event: GitEvent,
        originYaml: String?,
        filePath: String,
        changeSet: Set<String>?
    ): Boolean {
        if (originYaml.isNullOrBlank()) {
            return false
        }

        val (isTrigger, isTiming) = triggerMatcher.isMatch(
            event = event,
            gitRequestEvent = gitRequestEvent,
            pipeline = gitProjectPipeline,
            originYaml = originYaml
        )

        if (!isTrigger && !isTiming) {
            logger.warn(
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

        val yamlObjects = prepareCIBuildYaml(
            gitToken = gitToken,
            forkGitToken = forkGitToken,
            gitRequestEvent = gitRequestEvent,
            isMr = (event is GitMergeRequestEvent),
            originYaml = originYaml,
            filePath = filePath,
            pipelineId = gitProjectPipeline.pipelineId,
            pipelineName = gitProjectPipeline.displayName,
            event = event,
            changeSet = changeSet
        ) ?: return false
        val yamlObject = yamlObjects.normalYaml
        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        val parsedYaml = YamlCommonUtils.toYamlNotNull(yamlObjects.preYaml)
        logger.info("${gitProjectPipeline.pipelineId} parsedYaml: $parsedYaml")
        logger.info("normalize yaml: $normalizedYaml")

        // 若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称
        gitProjectPipeline.displayName = if (!yamlObject.name.isNullOrBlank()) {
            yamlObject.name!!
        } else {
            filePath.removeSuffix(".yml")
        }

        // 拼接插件时会需要传入GIT仓库信息需要提前刷新下状态
        gitBasicSettingService.refreshSetting(gitRequestEvent.gitProjectId)

        if (isTrigger) {
            // 正常匹配仓库操作触发
            logger.info(
                "Matcher is true, display the event, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                        "eventId: ${gitRequestEvent.id}, dispatched pipeline: $gitProjectPipeline"
            )
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
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
                gitBuildId = gitBuildId
            )
            return true
        }

        if (isTiming) {
            // 只有定时任务的保存任务
            logger.warn(
                "Only schedules matched, only save the pipeline, " +
                        "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
            )
            yamlBuildV2.gitStartBuild(
                pipeline = gitProjectPipeline,
                event = gitRequestEvent,
                yaml = yamlObject,
                parsedYaml = parsedYaml,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitBuildId = null
            )
        }
        return true
    }

    @Throws(TriggerBaseException::class, ErrorCodeException::class)
    override fun prepareCIBuildYaml(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        isMr: Boolean,
        originYaml: String?,
        filePath: String,
        pipelineId: String?,
        pipelineName: String?,
        event: GitEvent?,
        changeSet: Set<String>?
    ): YamlObjects? {
        if (originYaml.isNullOrBlank()) {
            return null
        }
        logger.info("input yamlStr: $originYaml")
        val isFork = (isMr) && gitRequestEvent.sourceGitProjectId != null &&
                gitRequestEvent.sourceGitProjectId != gitRequestEvent.gitProjectId
        val preTemplateYamlObject = yamlCheck.formatAndCheckYaml(
            originYaml = originYaml,
            gitRequestEvent = gitRequestEvent,
            filePath = filePath,
            isMr = isMr
        )
        return replaceYamlTemplate(
            isFork = isFork,
            isMr = isMr,
            gitToken = gitToken,
            forkGitToken = forkGitToken,
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
            yamlCheck.formatAndCheckYaml(yaml)

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
        gitToken: GitToken,
        forkGitToken: GitToken?,
        preTemplateYamlObject: PreTemplateScriptBuildYaml,
        filePath: String,
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        event: GitEvent?,
        changeSet: Set<String>?
    ): YamlObjects {
        // 替换yaml文件中的模板引用
        try {
            val preYamlObject = YamlTemplate(
                yamlObject = preTemplateYamlObject,
                filePath = filePath,
                triggerProjectId = scmService.getProjectId(isFork, gitRequestEvent),
                triggerUserId = gitRequestEvent.userId,
                sourceProjectId = gitRequestEvent.gitProjectId,
                triggerRef = gitRequestEvent.branch,
                triggerToken = gitToken.accessToken,
                repo = null,
                repoTemplateGraph = TemplateGraph(),
                getTemplateMethod = yamlTemplateService::getTemplate,
                forkGitToken = forkGitToken?.accessToken,
                changeSet = changeSet,
                event = event
            ).replace()
            return YamlObjects(
                preYaml = preYamlObject,
                normalYaml = ScriptYmlUtils.normalizeGitCiYaml(preYamlObject, filePath)
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
