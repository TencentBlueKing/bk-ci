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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.gitci.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_PUSH
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.listener.GitCIRequestDispatcher
import com.tencent.devops.gitci.listener.GitCIRequestTriggerEvent
import com.tencent.devops.gitci.pojo.EnvironmentVariables
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.utils.GitCIWebHookMatcher
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.TriggerBuildReq
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitCommit
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.GitFileInfo
import org.joda.time.DateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import javax.ws.rs.core.Response

@Service
class GitCITriggerService @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val gitCISettingDao: GitCISettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val repositoryConfService: RepositoryConfService,
    private val rabbitTemplate: RabbitTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerService::class.java)
        private val channelCode = ChannelCode.GIT
        private const val ciFileName = ".ci.yml"
        private const val ciFileDirectoryName = ".ci"
        private const val ciFileExtension = ".yml"
    }

    fun triggerBuild(userId: String, pipelineId: String, triggerBuildReq: TriggerBuildReq): Boolean {
        logger.info("Trigger build, userId: $userId, pipeline: $pipelineId, triggerBuildReq: $triggerBuildReq")

        val gitRequestEvent = createGitRequestEvent(userId, triggerBuildReq)
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

        val originYaml = triggerBuildReq.yaml
        val (yamlObject, normalizedYaml) = prepareCIBuildYaml(gitRequestEvent, originYaml) ?: return false

        val existsPipeline = gitPipelineResourceDao.getPipelineById(dslContext, triggerBuildReq.gitProjectId, pipelineId) ?: throw OperationException("git ci pipelineId not exist")
        // 如果该流水线已保存过，则继续使用
        val buildPipeline = GitProjectPipeline(
            gitProjectId = existsPipeline.gitProjectId,
            pipelineId = existsPipeline.pipelineId,
            filePath = existsPipeline.filePath,
            displayName = existsPipeline.displayName,
            enabled = existsPipeline.enabled,
            creator = existsPipeline.creator,
            latestBuildInfo = null
        )

        val gitBuildId = gitRequestEventBuildDao.save(
            dslContext = dslContext,
            eventId = gitRequestEvent.id!!,
            originYaml = originYaml!!,
            normalizedYaml = normalizedYaml,
            gitProjectId = gitRequestEvent.gitProjectId,
            branch = gitRequestEvent.branch,
            objectKind = gitRequestEvent.objectKind,
            description = triggerBuildReq.customCommitMsg
        )
        dispatchEvent(GitCIRequestTriggerEvent(buildPipeline, gitRequestEvent, yamlObject, gitBuildId))
        return true
    }

    fun externalCodeGitBuild(token: String, e: String): Boolean {
        logger.info("Trigger code git build($e)")

        val event = try {
            objectMapper.readValue<GitEvent>(e)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
            return false
        }

        val gitRequestEvent = saveGitRequestEvent(event, e) ?: return true
        return matchAndTriggerPipeline(gitRequestEvent, event)
    }

    private fun matchAndTriggerPipeline(gitRequestEvent: GitRequestEvent, event: GitEvent): Boolean {
        if (!checkGitProjectConf(gitRequestEvent, event)) return false
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId) ?: throw OperationException("git ci projectCode not exist")
        val name2PipelineExists = gitPipelineResourceDao.getAllByGitProjectId(dslContext, gitProjectConf.gitProjectId)
            .map { it.displayName to GitProjectPipeline(
                gitProjectId = it.gitProjectId,
                pipelineId = it.pipelineId,
                filePath = it.filePath,
                displayName = it.displayName,
                enabled = it.enabled,
                creator = it.creator,
                latestBuildInfo = null
            ) }.toMap()

        // 获取指定目录下所有yml文件
        val yamlPathList = getCIYamlList(gitRequestEvent)
        // 兼容旧的根目录yml文件
        yamlPathList.add(ciFileName)
        logger.info("matchAndTriggerPipeline in gitProjectId:${gitProjectConf.gitProjectId}, yamlPathList: $yamlPathList")

        var hasTriggered = false
        yamlPathList.forEach { path ->
            val originYaml = getYamlFromGit(gitRequestEvent, path)
            val (yamlObject, normalizedYaml) = prepareCIBuildYaml(gitRequestEvent, originYaml) ?: return@forEach
            val displayName = if (!yamlObject.name.isNullOrBlank()) yamlObject.name!! else path.removeSuffix(ciFileExtension)
            val existsPipeline = name2PipelineExists[displayName]

            // 如果该流水线已保存过，则继续使用
            val buildPipeline = existsPipeline
                ?: GitProjectPipeline(
                    gitProjectId = gitProjectConf.gitProjectId,
                    displayName = displayName,
                    pipelineId = "", // 留空用于是否创建判断
                    filePath = path,
                    enabled = true,
                    creator = gitRequestEvent.userId,
                    latestBuildInfo = null
                )

            val matcher = GitCIWebHookMatcher(event)

            // 流水线未启用则跳过
            if (!buildPipeline.enabled) {
                logger.warn("Pipeline is not enabled, return, eventId: ${gitRequestEvent.id}")
                gitRequestEventNotBuildDao.save(
                    dslContext = dslContext,
                    eventId = gitRequestEvent.id!!,
                    originYaml = originYaml,
                    normalizedYaml = normalizedYaml,
                    reason = TriggerReason.PIPELINE_DISABLE.name,
                    gitprojectId = gitRequestEvent.gitProjectId
                )
                return@forEach
            }

            if (matcher.isMatch(yamlObject.trigger!!, yamlObject.mr!!)) {
                logger.info("Matcher is true, display the event, eventId: ${gitRequestEvent.id}, dispatched pipeline: $buildPipeline")
                val gitBuildId = gitRequestEventBuildDao.save(
                    dslContext = dslContext,
                    eventId = gitRequestEvent.id!!,
                    originYaml = originYaml!!,
                    normalizedYaml = normalizedYaml,
                    gitProjectId = gitRequestEvent.gitProjectId,
                    branch = gitRequestEvent.branch,
                    objectKind = gitRequestEvent.objectKind,
                    description = gitRequestEvent.commitMsg
                )
                repositoryConfService.updateGitCISetting(gitRequestEvent.gitProjectId)
                dispatchEvent(GitCIRequestTriggerEvent(buildPipeline, gitRequestEvent, yamlObject, gitBuildId))
                hasTriggered = true
            } else {
                logger.warn("Matcher is false, return, eventId: ${gitRequestEvent.id}")
                gitRequestEventNotBuildDao.save(
                    dslContext = dslContext,
                    eventId = gitRequestEvent.id!!,
                    originYaml = originYaml,
                    normalizedYaml = normalizedYaml,
                    reason = TriggerReason.TRIGGER_NOT_MATCH.name,
                    gitprojectId = gitRequestEvent.gitProjectId
                )
            }
        }
        return hasTriggered
    }

    fun validateCIBuildYaml(yamlStr: String) = CiYamlUtils.validateYaml(yamlStr)

    fun getCIBuildYamlSchema() = CiYamlUtils.getCIBuildYamlSchema()

    fun createCIBuildYaml(yamlStr: String, gitProjectId: Long? = null): CIBuildYaml {
        logger.info("input yamlStr: $yamlStr")

        var yaml = CiYamlUtils.formatYaml(yamlStr)
        yaml = replaceEnv(yaml, gitProjectId)
        val yamlObject = YamlUtil.getObjectMapper().readValue(yaml, CIBuildYaml::class.java)

        // 检测services镜像
        if (yamlObject.services != null) {
            yamlObject.services!!.forEachIndexed { index, it ->
                // 判断镜像格式是否合法
                val (imageName, imageTag) = it.parseImage()
                val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                    ?: throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "Git CI没有此镜像版本记录. ${it.image}")
                if (!record.enable) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "镜像版本不可用. ${it.image}")
                }
            }
        }

        return CiYamlUtils.normalizeGitCiYaml(yamlObject)
    }

    private fun prepareCIBuildYaml(gitRequestEvent: GitRequestEvent, originYaml: String?): Pair<CIBuildYaml, String>? {

        if (originYaml.isNullOrBlank()) {
            logger.error("gitProjectId: ${gitRequestEvent.gitProjectId} get ci yaml from git return null")
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = null,
                normalizedYaml = null,
                reason = TriggerReason.GIT_CI_YAML_NOT_FOUND.name,
                gitprojectId = gitRequestEvent.gitProjectId
            )
            return null
        }

        val yamlObject = try {
            createCIBuildYaml(originYaml!!, gitRequestEvent.gitProjectId)
        } catch (e: Throwable) {
            logger.error("git ci yaml is invalid", e)
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.GIT_CI_YAML_INVALID.name,
                gitprojectId = gitRequestEvent.gitProjectId
            )
            return null
        }

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")
        return Pair(yamlObject, normalizedYaml)
    }

    private fun checkGitProjectConf(gitRequestEvent: GitRequestEvent, event: GitEvent): Boolean {
        val gitProjectSetting = gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId)
        if (null == gitProjectSetting) {
            logger.info("git ci is not enabled, git project id: ${gitRequestEvent.gitProjectId}")
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = null,
                normalizedYaml = null,
                reason = TriggerReason.GIT_CI_DISABLE.name,
                gitprojectId = gitRequestEvent.gitProjectId
            )
            return false
        }
        if (!gitProjectSetting.enableCi) {
            logger.warn("git ci is disabled, git project id: ${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
            gitRequestEventNotBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = null,
                normalizedYaml = null,
                reason = "git ci config is not enabled",
                gitprojectId = gitRequestEvent.gitProjectId
            )
            return false
        }
        when (event) {
            is GitPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn("git ci conf buildPushedBranches is false, git project id: ${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    gitRequestEventNotBuildDao.save(
                        dslContext = dslContext,
                        eventId = gitRequestEvent.id!!,
                        originYaml = null,
                        normalizedYaml = null,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLE.name,
                        gitprojectId = gitRequestEvent.gitProjectId
                    )
                    return false
                }
            }
            is GitTagPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn("git ci conf buildPushedBranches is false, git project id: ${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    gitRequestEventNotBuildDao.save(
                        dslContext = dslContext,
                        eventId = gitRequestEvent.id!!,
                        originYaml = null,
                        normalizedYaml = null,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLE.name,
                        gitprojectId = gitRequestEvent.gitProjectId
                    )
                    return false
                }
            }
            is GitMergeRequestEvent -> {
                if (!gitProjectSetting.buildPushedPullRequest) {
                    logger.warn("git ci conf buildPushedPullRequest is false, git project id: ${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    gitRequestEventNotBuildDao.save(
                        dslContext = dslContext,
                        eventId = gitRequestEvent.id!!,
                        originYaml = null,
                        normalizedYaml = null,
                        reason = TriggerReason.BUILD_PUSHED_PULL_REQUEST_DISABLE.name,
                        gitprojectId = gitRequestEvent.gitProjectId
                    )
                    return false
                }
            }
        }

        return true
    }

    private fun replaceEnv(yaml: String, gitProjectId: Long?): String {
        if (gitProjectId == null) {
            return yaml
        }
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: return yaml
        logger.info("gitProjectConf: $gitProjectConf")
        if (null == gitProjectConf.env) {
            return yaml
        }

        val sb = StringBuilder()
        val br = BufferedReader(StringReader(yaml))
        val envRegex = Regex("\\\$env:\\w+")
        var line: String? = br.readLine()
        while (line != null) {
            val envMatches = envRegex.find(line)
            envMatches?.groupValues?.forEach {
                logger.info("envKeyPrefix: $it")
                val envKeyPrefix = it
                val envKey = envKeyPrefix.removePrefix("\$env:")
                val envValue = getEnvValue(gitProjectConf.env!!, envKey)
                logger.info("envKey: $envKey, envValue: $envValue")
                line = if (null != envValue) {
                    envRegex.replace(line!!, envValue)
                } else {
                    envRegex.replace(line!!, "null")
                }
                logger.info("line: $line")
            }

            sb.append(line).append("\n")
            line = br.readLine()
        }

        return sb.toString()
    }

    private fun getEnvValue(env: List<EnvironmentVariables>, key: String): String? {
        env.forEach {
            if (it.name == key) {
                return it.value
            }
        }
        return null
    }

    private fun dispatchEvent(event: GitCIRequestTriggerEvent) {
        GitCIRequestDispatcher.dispatch(rabbitTemplate, event)
    }

    private fun getYamlFromGit(gitRequestEvent: GitRequestEvent, fileName: String): String? {
        return try {
            val gitToken = client.getScm(ServiceGitResource::class).getToken(gitRequestEvent.gitProjectId).data!!
            logger.info("get token form scm, token: $gitToken")
            val ref = when {
                gitRequestEvent.branch.startsWith("refs/heads/") -> gitRequestEvent.branch.removePrefix("refs/heads/")
                gitRequestEvent.branch.startsWith("refs/tags/") -> gitRequestEvent.branch.removePrefix("refs/tags/")
                else -> gitRequestEvent.branch
            }
            val result = client.getScm(ServiceGitResource::class).getGitCIFileContent(gitRequestEvent.gitProjectId, fileName, gitToken.accessToken, ref)
            result.data
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            null
        }
    }

    private fun getCIYamlList(gitRequestEvent: GitRequestEvent): MutableList<String> {
        val ciFileList = getFileTreeFromGit(gitRequestEvent, ciFileDirectoryName)
            .filter { it.name.endsWith(ciFileExtension) }
        return ciFileList.map { ciFileDirectoryName + File.separator + it.name }.toMutableList()
    }

    private fun getFileTreeFromGit(gitRequestEvent: GitRequestEvent, filePath: String): List<GitFileInfo> {
        return try {
            val gitToken = client.getScm(ServiceGitResource::class).getToken(gitRequestEvent.gitProjectId).data!!
            logger.info("get token form scm, token: $gitToken")
            val ref = when {
                gitRequestEvent.branch.startsWith("refs/heads/") -> gitRequestEvent.branch.removePrefix("refs/heads/")
                gitRequestEvent.branch.startsWith("refs/tags/") -> gitRequestEvent.branch.removePrefix("refs/tags/")
                else -> gitRequestEvent.branch
            }
            val result = client.getScm(ServiceGitResource::class).getGitCIFileTree(gitRequestEvent.gitProjectId, filePath, gitToken.accessToken, ref)
            result.data!!
        } catch (e: Throwable) {
            logger.error("Get yaml from git failed", e)
            emptyList()
        }
    }

    private fun saveGitRequestEvent(event: GitEvent, e: String): GitRequestEvent? {
        when (event) {
            is GitPushEvent -> {
                if (event.total_commits_count <= 0) {
                    logger.info("Git web hook no commit(${event.total_commits_count})")
                    return null
                }
                val gitRequestEvent = createGitRequestEvent(event, e)
                val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
                gitRequestEvent.id = id
                return gitRequestEvent
            }
            is GitTagPushEvent -> {
                if (event.total_commits_count <= 0) {
                    logger.info("Git web hook no commit(${event.total_commits_count})")
                    return null
                }
                val gitRequestEvent = createGitRequestEvent(event, e)
                val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
                gitRequestEvent.id = id
                return gitRequestEvent
            }
            is GitMergeRequestEvent -> {
                if (event.object_attributes.action == "close" || event.object_attributes.action == "merge" ||
                        (event.object_attributes.action == "update" && event.object_attributes.extension_action != "push-update")
                ) {
                    logger.info("Git web hook is ${event.object_attributes.action} merge request")
                    return null
                }
                val gitRequestEvent = createGitRequestEvent(event, e)
                val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
                gitRequestEvent.id = id
                return gitRequestEvent
            }
        }
        logger.info("event invalid: $event")
        return null
    }

    private fun createGitRequestEvent(gitPushEvent: GitPushEvent, e: String): GitRequestEvent {
        val latestCommit = getLatestCommit(gitPushEvent.after, gitPushEvent.commits)
        return GitRequestEvent(
            id = null,
            objectKind = OBJECT_KIND_PUSH,
            operationKind = gitPushEvent.operation_kind,
            extensionAction = null,
            gitProjectId = gitPushEvent.project_id,
            branch = gitPushEvent.ref.removePrefix("refs/heads/"),
            targetBranch = null,
            commitId = gitPushEvent.after,
            commitMsg = latestCommit?.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit?.timestamp),
            userId = gitPushEvent.user_name,
            totalCommitCount = gitPushEvent.total_commits_count.toLong(),
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null
        )
    }

    private fun createGitRequestEvent(gitTagPushEvent: GitTagPushEvent, e: String): GitRequestEvent {
        val latestCommit = getLatestCommit(gitTagPushEvent.after, gitTagPushEvent.commits)
        return GitRequestEvent(
            id = null,
            objectKind = OBJECT_KIND_TAG_PUSH,
            operationKind = gitTagPushEvent.operation_kind,
            extensionAction = null,
            gitProjectId = gitTagPushEvent.project_id,
            branch = gitTagPushEvent.ref.removePrefix("refs/tags/"),
            targetBranch = null,
            commitId = gitTagPushEvent.after,
            commitMsg = latestCommit?.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit?.timestamp),
            userId = gitTagPushEvent.user_name,
            totalCommitCount = gitTagPushEvent.total_commits_count.toLong(),
            mergeRequestId = null,
            event = e,
            description = "",
            mrTitle = null
        )
    }

    private fun createGitRequestEvent(gitMrEvent: GitMergeRequestEvent, e: String): GitRequestEvent {
        val latestCommit = gitMrEvent.object_attributes.last_commit
        return GitRequestEvent(
            id = null,
            objectKind = OBJECT_KIND_MERGE_REQUEST,
            operationKind = null,
            extensionAction = gitMrEvent.object_attributes.extension_action,
            gitProjectId = gitMrEvent.object_attributes.source_project_id,
            branch = gitMrEvent.object_attributes.source_branch,
            targetBranch = gitMrEvent.object_attributes.target_branch,
            commitId = latestCommit.id,
            commitMsg = latestCommit.message,
            commitTimeStamp = getCommitTimeStamp(latestCommit.timestamp),
            userId = gitMrEvent.user.username,
            totalCommitCount = 0,
            mergeRequestId = gitMrEvent.object_attributes.iid,
            event = e,
            description = "",
            mrTitle = gitMrEvent.object_attributes.title
        )
    }

    private fun createGitRequestEvent(userId: String, triggerBuildReq: TriggerBuildReq): GitRequestEvent {
        return GitRequestEvent(
            id = null,
            objectKind = OBJECT_KIND_MANUAL,
            operationKind = "",
            extensionAction = null,
            gitProjectId = triggerBuildReq.gitProjectId,
            branch = triggerBuildReq.branch.removePrefix("refs/heads/"),
            targetBranch = null,
            commitId = "",
            commitMsg = triggerBuildReq.customCommitMsg,
            commitTimeStamp = getCommitTimeStamp(null),
            userId = userId,
            totalCommitCount = 0,
            mergeRequestId = null,
            event = "",
            description = triggerBuildReq.description,
            mrTitle = ""
        )
    }

    private fun getLatestCommit(commitId: String, commits: List<GitCommit>): GitCommit? {
        commits.forEach {
            if (it.id == commitId) {
                return it
            }
        }
        return null
    }

    private fun getCommitTimeStamp(commitTimeStamp: String?): String {
        return if (commitTimeStamp.isNullOrBlank()) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            formatter.format(Date())
        } else {
            val time = DateTime.parse(commitTimeStamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            sdf.format(time.toDate())
        }
    }

    fun getYaml(gitProjectId: Long, buildId: String): String {
        logger.info("get yaml by buildId:($buildId), gitProjectId: $gitProjectId")
        gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId)
        return (eventBuild?.originYaml) ?: ""
    }
}
