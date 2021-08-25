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

package com.tencent.devops.gitci.trigger.v2

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.exception.YamlFormatException
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.gitci.common.exception.CommitCheck
import com.tencent.devops.gitci.common.exception.TriggerBaseException
import com.tencent.devops.gitci.common.exception.TriggerException.Companion.triggerError
import com.tencent.devops.gitci.common.exception.YamlBlankException
import com.tencent.devops.gitci.common.exception.Yamls
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.GitCICommitCheckState
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.v2.YamlObjects
import com.tencent.devops.gitci.trigger.YamlTriggerInterface
import com.tencent.devops.gitci.v2.common.CommonConst
import com.tencent.devops.gitci.trigger.GitCIEventService
import com.tencent.devops.gitci.v2.service.ScmService
import com.tencent.devops.gitci.trigger.template.YamlTemplate
import com.tencent.devops.gitci.trigger.template.YamlTemplateService
import com.tencent.devops.gitci.trigger.template.pojo.NoReplaceTemplate
import com.tencent.devops.gitci.trigger.template.pojo.TemplateGraph
import com.tencent.devops.gitci.v2.service.GitCIBasicSettingService
import com.tencent.devops.gitci.v2.utils.V2WebHookMatcher
import com.tencent.devops.repository.pojo.oauth.GitToken
import io.jsonwebtoken.io.IOException
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
    private val gitCIEventSaveService: GitCIEventService,
    private val yamlTemplateService: YamlTemplateService,
    private val v2WebHookMatcher: V2WebHookMatcher,
    private val redisOperation: RedisOperation,
    private val yamlBuildV2: YamlBuildV2
) : YamlTriggerInterface<YamlObjects> {

    companion object {
        private val logger = LoggerFactory.getLogger(YamlTriggerV2::class.java)
        private const val ymlVersion = "v2.0"

        // 针对filePath可能为空的情况下创建一个模板替换的根目录名称
        private const val GIT_CI_TEMPLATE_ROOT_FILE = "GIT_CI_TEMPLATE_ROOT_FILE"
    }

    override fun triggerBuild(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        gitProjectPipeline: GitProjectPipeline,
        event: GitEvent,
        originYaml: String?,
        filePath: String
    ): Boolean {
        if (originYaml.isNullOrBlank()) {
            return false
        }
        // 触发器需要将 on: 转为 TriggeriOn:
        val (isTrigger, isTiming) = isMatch(
            event = event,
            gitRequestEvent = gitRequestEvent,
            pipeline = gitProjectPipeline,
            originYaml = try {
                ScriptYmlUtils.formatYaml(originYaml)
            } catch (e: JsonProcessingException) {
                triggerError(
                    request = gitRequestEvent,
                    event = event,
                    pipeline = gitProjectPipeline,
                    reason = TriggerReason.CI_YAML_INVALID,
                    reasonParams = listOf(e.message ?: ""),
                    yamls = Yamls(originYaml, null, null),
                    commitCheck = CommitCheck(
                        block = event is GitMergeRequestEvent,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            } catch (e: TypeCastException) {
                triggerError(
                    request = gitRequestEvent,
                    event = event,
                    pipeline = gitProjectPipeline,
                    reason = TriggerReason.CI_YAML_INVALID,
                    reasonParams = listOf(e.message ?: ""),
                    yamls = Yamls(originYaml, null, null),
                    commitCheck = CommitCheck(
                        block = event is GitMergeRequestEvent,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            }
        )

        if (!isTrigger && !isTiming) {
            logger.warn("Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                "eventId: ${gitRequestEvent.id}")
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
            pipelineName = gitProjectPipeline.displayName
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
            logger.warn("Only schedules matched, only save the pipeline, " +
                "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}")
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

    fun isMatch(
        event: GitEvent,
        gitRequestEvent: GitRequestEvent,
        pipeline: GitProjectPipeline,
        originYaml: String
    ): Pair<Boolean, Boolean> {
        val newYaml =
            try {
                YamlUtil.getObjectMapper()
                    .readValue(originYaml, object : TypeReference<NoReplaceTemplate>() {})
            } catch (e: JsonProcessingException) {
                triggerError(
                    request = gitRequestEvent,
                    event = event,
                    pipeline = pipeline,
                    reason = TriggerReason.CI_YAML_INVALID,
                    reasonParams = listOf(e.message ?: ""),
                    yamls = Yamls(originYaml, null, null),
                    commitCheck = CommitCheck(
                        block = event is GitMergeRequestEvent,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            }

        return v2WebHookMatcher.isMatch(
            triggerOn = ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn),
            event = event,
            gitRequestEvent = gitRequestEvent
        )
    }

    override fun prepareCIBuildYaml(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        isMr: Boolean,
        originYaml: String?,
        filePath: String,
        pipelineId: String?,
        pipelineName: String?
    ): YamlObjects? {
        if (originYaml.isNullOrBlank()) {
            return null
        }
        logger.info("input yamlStr: $originYaml")
        val isFork = (isMr) && gitRequestEvent.sourceGitProjectId != null &&
            gitRequestEvent.sourceGitProjectId != gitRequestEvent.gitProjectId
        val preTemplateYamlObject = formatAndCheckYaml(
            originYaml = originYaml,
            gitRequestEvent = gitRequestEvent,
            pipelineId = pipelineId,
            filePath = filePath,
            isMr = isMr,
            pipelineName = pipelineName
        ) ?: return null
        return replaceYamlTemplate(
            isFork = isFork,
            isMr = isMr,
            gitToken = gitToken,
            forkGitToken = forkGitToken,
            preTemplateYamlObject = preTemplateYamlObject,
            filePath = filePath ?: GIT_CI_TEMPLATE_ROOT_FILE,
            gitRequestEvent = gitRequestEvent,
            pipelineId = pipelineId,
            originYaml = originYaml,
            pipelineName = pipelineName
        )
    }

    override fun checkYamlSchema(userId: String, yaml: String): Result<String> {
        return try {
            checkYamlSchema(yaml)

            Result("OK")
        } catch (e: Exception) {
            logger.error("Check yaml schema failed.", e)
            Result(1, "Invalid yaml: ${e.message}")
        }
    }

    @Throws(
        JsonProcessingException::class,
        RuntimeException::class,
        IOException::class,
        Exception::class,
        YamlFormatException::class
    )
    private fun checkYamlSchema(yaml: String): PreTemplateScriptBuildYaml {
        val formatYamlStr = ScriptYmlUtils.formatYaml(yaml)
        val yamlJsonStr = ScriptYmlUtils.convertYamlToJson(formatYamlStr)

        val gitciYamlSchema = redisOperation.get(CommonConst.REDIS_GITCI_YAML_SCHEMA)
            ?: throw RuntimeException("Check Schema is null.")

        val (schemaPassed, errorMessage) = ScriptYmlUtils.validate(
            schema = gitciYamlSchema,
            yamlJson = yamlJsonStr
        )

        // 先做总体的schema校验
        if (!schemaPassed) {
            logger.warn("Check yaml schema failed. $errorMessage")
            throw YamlFormatException(errorMessage)
        }

        val preTemplateYamlObject =
            YamlUtil.getObjectMapper().readValue(formatYamlStr, PreTemplateScriptBuildYaml::class.java)
        // 检查Yaml语法的格式问题
        ScriptYmlUtils.checkYaml(preTemplateYamlObject, yaml)

        return preTemplateYamlObject
    }

    private fun formatAndCheckYaml(
        originYaml: String,
        gitRequestEvent: GitRequestEvent,
        pipelineId: String?,
        pipelineName: String?,
        filePath: String,
        isMr: Boolean
    ): PreTemplateScriptBuildYaml? {
        return try {
            checkYamlSchema(originYaml)
        } catch (e: Throwable) {
            logger.info("gitRequestEvent ${gitRequestEvent.id} git ci yaml is invalid", e)
            val (block, message, reason) = when (e) {
                is YamlFormatException, is CustomException -> {
                    Triple(isMr, e.message, TriggerReason.CI_YAML_INVALID)
                }
                is IOException, is TypeCastException -> {
                    Triple(isMr, e.message, TriggerReason.CI_YAML_INVALID)
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

    private fun replaceYamlTemplate(
        isFork: Boolean,
        isMr: Boolean,
        gitToken: GitToken,
        forkGitToken: GitToken?,
        preTemplateYamlObject: PreTemplateScriptBuildYaml,
        filePath: String,
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        pipelineId: String?,
        pipelineName: String?
    ): YamlObjects? {
        // 替换yaml文件中的模板引用
        try {
            val preYamlObject = YamlTemplate(
                yamlObject = preTemplateYamlObject,
                filePath = filePath,
                triggerProjectId = scmService.getProjectId(isFork, gitRequestEvent),
                triggerUserId = gitRequestEvent.userId,
                sourceProjectId = gitRequestEvent.gitProjectId,
                triggerRef = gitRequestEvent.branch,
                triggerToken = if (isFork) {
                    forkGitToken!!.accessToken
                } else {
                    gitToken.accessToken
                },
                repo = null,
                repoTemplateGraph = TemplateGraph(),
                getTemplateMethod = yamlTemplateService::getTemplate
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
