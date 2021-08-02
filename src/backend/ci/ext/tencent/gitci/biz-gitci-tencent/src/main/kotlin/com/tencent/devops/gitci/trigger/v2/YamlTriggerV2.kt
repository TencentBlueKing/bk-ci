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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.v2.YamlObjects
import com.tencent.devops.gitci.service.GitRepositoryConfService
import com.tencent.devops.gitci.trigger.YamlTriggerInterface
import com.tencent.devops.gitci.v2.common.CommonConst
import com.tencent.devops.gitci.trigger.GitCIEventService
import com.tencent.devops.gitci.v2.service.ScmService
import com.tencent.devops.gitci.trigger.template.YamlTemplate
import com.tencent.devops.gitci.trigger.template.YamlTemplateService
import com.tencent.devops.gitci.trigger.template.pojo.TemplateGraph
import com.tencent.devops.gitci.v2.utils.V2WebHookMatcher
import com.tencent.devops.repository.pojo.oauth.GitToken
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
class YamlTriggerV2 @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmService: ScmService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitBasicSettingService: GitRepositoryConfService,
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
        gitProjectPipeline.displayName =
            if (!yamlObject.name.isNullOrBlank()) yamlObject.name!! else filePath.removeSuffix(".yml")

        val matchResult = isMatch(event, gitRequestEvent, yamlObjects)
        if (matchResult.first) {
            // 正常匹配仓库操作触发
            logger.info(
                "Matcher is true, display the event, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                    "eventId: ${gitRequestEvent.id}, dispatched pipeline: $gitProjectPipeline"
            )
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml!!,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                description = gitRequestEvent.commitMsg,
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
            gitBasicSettingService.updateGitCISetting(gitRequestEvent.gitProjectId)
        } else if (matchResult.second) {
            // 只有定时任务的保存任务
            logger.warn("Only schedules matched, only save the pipeline, " +
                "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}")
            yamlBuildV2.gitStartBuild(
                pipeline = gitProjectPipeline,
                event = gitRequestEvent,
                yaml = yamlObject,
                parsedYaml = parsedYaml,
                originYaml = originYaml!!,
                normalizedYaml = normalizedYaml,
                gitBuildId = null
            )
        } else {
            logger.warn("Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                "eventId: ${gitRequestEvent.id}")
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = if (gitProjectPipeline.pipelineId.isBlank()) null else gitProjectPipeline.pipelineId,
                pipelineName = gitProjectPipeline.displayName,
                filePath = gitProjectPipeline.filePath,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                reason = TriggerReason.TRIGGER_NOT_MATCH.name,
                reasonDetail = TriggerReason.TRIGGER_NOT_MATCH.detail,
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false,
                version = ymlVersion
            )
        }
        return true
    }

    override fun isMatch(
        event: GitEvent,
        gitRequestEvent: GitRequestEvent,
        ymlObject: YamlObjects
    ): Pair<Boolean, Boolean> {
        return v2WebHookMatcher.isMatch(ymlObject.normalYaml.triggerOn!!, event, gitRequestEvent)
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

    private fun checkYamlSchema(yaml: String): PreTemplateScriptBuildYaml {
        val formatYamlStr = ScriptYmlUtils.formatYaml(yaml)
        val yamlJsonStr = ScriptYmlUtils.convertYamlToJson(formatYamlStr)

        val gitciYamlSchema = redisOperation.get(CommonConst.REDIS_GITCI_YAML_SCHEMA)
            ?: throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "Check Schema is null.")

        val (schemaPassed, errorMessage) = ScriptYmlUtils.validate(
            schema = gitciYamlSchema,
            yamlJson = yamlJsonStr
        )

        // 先做总体的schema校验
        if (!schemaPassed) {
            logger.error("Check yaml schema failed. $errorMessage")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage)
        }

        return try {
            val preTemplateYamlObject =
                YamlUtil.getObjectMapper().readValue(formatYamlStr, PreTemplateScriptBuildYaml::class.java)
            // 检查Yaml语法的格式问题
            ScriptYmlUtils.checkYaml(preTemplateYamlObject, yaml)

            preTemplateYamlObject
        } catch (e: Exception) {
            logger.error("Check yaml schema failed.", e)
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, e.message ?: "Check yaml schema failed.")
        }
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
        } catch (e: Exception) {
            logger.error("git ci yaml is invalid", e)
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                originYaml = originYaml,
                parsedYaml = null,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_INVALID.name,
                reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message),
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = true,
                commitCheckBlock = isMr,
                version = ymlVersion
            )
            null
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
        val preYamlObject = try {
            YamlTemplate(
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
        } catch (e: Exception) {
            logger.error("git ci yaml template replace error", e)
            val message = if (e is StackOverflowError) {
                "Yaml file has circular dependency"
            } else {
                e.message.toString()
            }
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                filePath = filePath,
                originYaml = originYaml,
                parsedYaml = null,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_TEMPLATE_ERROR.name,
                reasonDetail = TriggerReason.CI_YAML_TEMPLATE_ERROR.detail.format(message),
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = true,
                commitCheckBlock = isMr,
                version = ymlVersion
            )
            return null
        }

        return YamlObjects(
            preYaml = preYamlObject,
            normalYaml = ScriptYmlUtils.normalizeGitCiYaml(preYamlObject, filePath)
        )
    }
}
