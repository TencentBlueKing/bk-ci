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

package com.tencent.devops.gitci.v2.service.trigger

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlTemplateUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.service.GitRepositoryConfService
import com.tencent.devops.gitci.service.trigger.RequestTriggerInterface
import com.tencent.devops.gitci.v2.listener.V2GitCIRequestDispatcher
import com.tencent.devops.gitci.v2.listener.V2GitCIRequestTriggerEvent
import com.tencent.devops.gitci.v2.utils.V2WebHookMatcher
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.GitFileInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@Component
class V2RequestTrigger @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val repositoryConfService: GitRepositoryConfService,
    private val rabbitTemplate: RabbitTemplate
) : RequestTriggerInterface<ScriptBuildYaml> {

    companion object {
        private val logger = LoggerFactory.getLogger(V2RequestTrigger::class.java)
        private const val ciFileName = ".ci.yml"
        private const val templateDirectoryName = ".ci/templates"
        private const val ciFileExtension = ".yml"
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
        val yamlObject = prepareCIBuildYaml(
            gitToken = gitToken,
            forkGitToken = forkGitToken,
            gitRequestEvent = gitRequestEvent,
            event = event,
            originYaml = originYaml,
            filePath = filePath,
            pipelineId = gitProjectPipeline.pipelineId
        ) ?: return false

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")

        // 若是Yaml格式没问题，则取Yaml中的流水线名称，并修改当前流水线名称
        gitProjectPipeline.displayName =
            if (!yamlObject.name.isNullOrBlank()) yamlObject.name!! else filePath.removeSuffix(".yml")

        if (isMatch(event, yamlObject)) {
            logger.info("Matcher is true, display the event, gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}, dispatched pipeline: $gitProjectPipeline")
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml!!,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                description = gitRequestEvent.commitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId
            )
            V2GitCIRequestDispatcher.dispatch(
                rabbitTemplate,
                V2GitCIRequestTriggerEvent(
                    pipeline = gitProjectPipeline,
                    event = gitRequestEvent,
                    yaml = yamlObject,
                    originYaml = originYaml,
                    normalizedYaml = normalizedYaml,
                    gitBuildId = gitBuildId
                )
            )
            repositoryConfService.updateGitCISetting(gitRequestEvent.gitProjectId)
        } else {
            logger.warn("Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}")
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                pipelineId = if (gitProjectPipeline.pipelineId.isBlank()) null else gitProjectPipeline.pipelineId,
                filePath = gitProjectPipeline.filePath,
                originYaml = originYaml,
                normalizedYaml = normalizedYaml,
                reason = TriggerReason.TRIGGER_NOT_MATCH.name,
                reasonDetail = TriggerReason.TRIGGER_NOT_MATCH.detail,
                gitProjectId = gitRequestEvent.gitProjectId
            )
        }

        return true
    }

    override fun isMatch(event: GitEvent, ymlObject: ScriptBuildYaml): Boolean {
        return V2WebHookMatcher(event).isMatch(ymlObject.triggerOn!!)
    }

    override fun prepareCIBuildYaml(
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        originYaml: String?,
        filePath: String?,
        pipelineId: String?
    ): ScriptBuildYaml? {
        if (originYaml.isNullOrBlank()) {
            return null
        }
        val isFork = (event is GitMergeRequestEvent) && gitRequestEvent.sourceGitProjectId != null &&
                gitRequestEvent.sourceGitProjectId != gitRequestEvent.gitProjectId
        val yamlObject = try {
            createCIBuildYaml(
                isFork = isFork,
                gitToken = gitToken,
                forkGitToken = forkGitToken,
                yamlStr = originYaml,
                filePath = filePath ?: "root",
                gitRequestEvent = gitRequestEvent,
                gitProjectId = gitRequestEvent.gitProjectId
            )
        } catch (e: Throwable) {
            logger.error("git ci yaml is invalid", e)
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.GIT_CI_YAML_INVALID.name,
                reasonDetail = e.message.toString(),
                gitProjectId = gitRequestEvent.gitProjectId
            )
            return null
        }

        return yamlObject
    }

    fun createCIBuildYaml(
        isFork: Boolean,
        gitToken: GitToken,
        forkGitToken: GitToken?,
        yamlStr: String,
        filePath: String,
        gitRequestEvent: GitRequestEvent,
        gitProjectId: Long? = null
    ): ScriptBuildYaml {
        logger.info("input yamlStr: $yamlStr")

        val yaml = ScriptYmlUtils.formatYaml(yamlStr)
        // 拉取所有的模板文件
        val templates = getAllTemplates(isFork, gitToken, forkGitToken, gitRequestEvent)
        val preTemplateYamlObject = YamlUtil.getObjectMapper().readValue(yaml, PreTemplateScriptBuildYaml::class.java)
        // 校验是否符合规范
        ScriptYmlUtils.checkStage(preTemplateYamlObject)
        // 替换yaml文件中的模板引用
        val preYamlObject = YamlTemplateUtils(preTemplateYamlObject, templates, filePath).replace()

        return ScriptYmlUtils.normalizeGitCiYaml(preYamlObject)
    }

    private fun getAllTemplates(
        isFork: Boolean,
        gitToken: GitToken,
        forkGitToken: GitToken?,
        gitRequestEvent: GitRequestEvent
    ): Map<String, String?> {
        val token = if (isFork) {
            forkGitToken!!
        } else {
            gitToken
        }
        val templateFileList = getCIYamlList(token, gitRequestEvent, isFork)
        val templates = mutableMapOf<String, String?>()
        templateFileList.forEach { filePath ->
            templates[filePath.removePrefix("$templateDirectoryName/")] = getYamlFromGit(
                gitToken = token,
                gitRequestEvent = gitRequestEvent,
                fileName = filePath,
                isMrEvent = isFork
            )
        }
        return templates
    }

    private fun getCIYamlList(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        isFork: Boolean = false
    ): MutableList<String> {
        val ciFileList = getFileTreeFromGit(gitToken, gitRequestEvent, templateDirectoryName, isFork)
            .filter { it.name.endsWith(ciFileExtension) }
        return ciFileList.map { templateDirectoryName + File.separator + it.name }.toMutableList()
    }

    private fun getFileTreeFromGit(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        filePath: String,
        isFork: Boolean = false
    ): List<GitFileInfo> {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileTree(
                gitProjectId = getProjectId(isFork, gitRequestEvent),
                path = filePath,
                token = gitToken.accessToken,
                ref = getTriggerBranch(gitRequestEvent)
            )
            result.data!!
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            emptyList()
        }
    }

    private fun getYamlFromGit(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        fileName: String,
        isMrEvent: Boolean = false
    ): String? {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileContent(
                gitProjectId = getProjectId(isMrEvent, gitRequestEvent),
                filePath = fileName,
                token = gitToken.accessToken,
                ref = getTriggerBranch(gitRequestEvent)
            )
            result.data
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            null
        }
    }

    // 获取项目ID，兼容没有source字段的旧数据，和fork库中源项目id不同的情况
    private fun getProjectId(isFork: Boolean = false, gitRequestEvent: GitRequestEvent): Long {
        with(gitRequestEvent) {
            return if (isFork) {
                sourceGitProjectId!!
            } else {
                gitProjectId
            }
        }
    }

    private fun getTriggerBranch(gitRequestEvent: GitRequestEvent): String {
        return when {
            gitRequestEvent.branch.startsWith("refs/heads/") -> gitRequestEvent.branch.removePrefix("refs/heads/")
            gitRequestEvent.branch.startsWith("refs/tags/") -> gitRequestEvent.branch.removePrefix("refs/tags/")
            else -> gitRequestEvent.branch
        }
//        return if (gitRequestEvent.objectKind == OBJECT_KIND_MERGE_REQUEST) {
//            when {
//                gitRequestEvent.targetBranch!!.startsWith("refs/heads/") -> gitRequestEvent.targetBranch!!.removePrefix("refs/heads/")
//                gitRequestEvent.targetBranch!!.startsWith("refs/tags/") -> gitRequestEvent.targetBranch!!.removePrefix("refs/tags/")
//                else -> gitRequestEvent.targetBranch!!
//            }
//        } else {
//            when {
//                gitRequestEvent.branch.startsWith("refs/heads/") -> gitRequestEvent.branch.removePrefix("refs/heads/")
//                gitRequestEvent.branch.startsWith("refs/tags/") -> gitRequestEvent.branch.removePrefix("refs/tags/")
//                else -> gitRequestEvent.branch
//            }
//        }
    }
}
