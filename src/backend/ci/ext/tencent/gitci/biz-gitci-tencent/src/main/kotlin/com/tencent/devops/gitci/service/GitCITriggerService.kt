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

package com.tencent.devops.gitci.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_PUSH
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.listener.GitCIMrConflictCheckDispatcher
import com.tencent.devops.gitci.listener.GitCIMrConflictCheckEvent
import com.tencent.devops.gitci.listener.GitCIRequestDispatcher
import com.tencent.devops.gitci.listener.GitCIRequestTriggerEvent
import com.tencent.devops.gitci.pojo.EnvironmentVariables
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.TriggerBuildReq
import com.tencent.devops.gitci.pojo.enums.GitCICommitCheckState
import com.tencent.devops.gitci.pojo.enums.GitCiMergeStatus
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitCommit
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting
import com.tencent.devops.gitci.pojo.v2.V2BuildYaml
import com.tencent.devops.gitci.service.trigger.RequestTriggerFactory
import com.tencent.devops.gitci.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.gitci.v2.listener.V2GitCIRequestDispatcher
import com.tencent.devops.gitci.v2.listener.V2GitCIRequestTriggerEvent
import com.tencent.devops.gitci.v2.service.GitCIEventSaveService
import com.tencent.devops.gitci.v2.service.OauthService
import com.tencent.devops.repository.pojo.oauth.GitToken
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
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import javax.ws.rs.core.Response

@Service
class GitCITriggerService @Autowired constructor(
    private val client: Client,
    private val scmClient: ScmClient,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val gitCISettingDao: GitCIBasicSettingDao,
    private val gitCIV1SettingDao: GitCISettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val rabbitTemplate: RabbitTemplate,
    private val requestTriggerFactory: RequestTriggerFactory,
    private val oauthService: OauthService,
    private val gitCIEventSaveService: GitCIEventSaveService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerService::class.java)
        private val channelCode = ChannelCode.GIT
        private val ciFileExtensions = listOf(".yml", ".yaml")
        private const val ciFileExtensionYml = ".yml"
        private const val ciFileExtensionYaml = ".yaml"
        private const val ciFileName = ".ci.yml"
        private const val ciFileDirectoryName = ".ci"
        private const val noPipelineBuildEvent = "MR held, waiting until pipeline validation finish."
    }

    fun triggerBuild(userId: String, pipelineId: String, triggerBuildReq: TriggerBuildReq): Boolean {
        logger.info("Trigger build, userId: $userId, pipeline: $pipelineId, triggerBuildReq: $triggerBuildReq")

        val gitRequestEvent = createGitRequestEvent(userId, triggerBuildReq)
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

        val existsPipeline =
            gitPipelineResourceDao.getPipelineById(dslContext, triggerBuildReq.gitProjectId, pipelineId)
                ?: throw OperationException("git ci pipelineId not exist")
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

        // 流水线未启用在手动触发处直接报错
        if (!buildPipeline.enabled) {
            logger.error(
                "Pipeline is not enabled, return, " +
                        "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
            )
            throw RuntimeException("${TriggerReason.PIPELINE_DISABLE.name}(${TriggerReason.PIPELINE_DISABLE.detail})")
        }

        val originYaml = triggerBuildReq.yaml
        // 如果当前文件没有内容直接不触发
        if (originYaml.isNullOrBlank()) {
            logger.warn("Matcher is false, return, gitProjectId: ${gitRequestEvent.gitProjectId}, " +
                "eventId: ${gitRequestEvent.id}")
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = if (buildPipeline.pipelineId.isBlank()) null else buildPipeline.pipelineId,
                filePath = buildPipeline.filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_CONTENT_NULL.name,
                reasonDetail = TriggerReason.CI_YAML_CONTENT_NULL.detail,
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false
            )
        }

        if (!ScriptYmlUtils.isV2Version(originYaml)) {
            val (yamlObject, normalizedYaml) =
                prepareCIBuildYaml(gitRequestEvent, originYaml, existsPipeline.filePath, existsPipeline.pipelineId)
                    ?: return false

            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml!!,
                parsedYaml = originYaml,
                normalizedYaml = normalizedYaml,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                description = triggerBuildReq.customCommitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
                buildStatus = BuildStatus.RUNNING
            )
            dispatchEvent(
                GitCIRequestTriggerEvent(
                    pipeline = buildPipeline,
                    event = gitRequestEvent,
                    yaml = yamlObject,
                    originYaml = originYaml,
                    normalizedYaml = normalizedYaml,
                    gitBuildId = gitBuildId
                )
            )
            return true
        } else {
            // v2 先做OAuth校验
            val triggerUser = gitRequestEvent.userId
            val token = oauthService.getAndCheckOauthToken(userId)
            val objects = requestTriggerFactory.v2RequestTrigger.prepareCIBuildYaml(
                gitToken = token,
                forkGitToken = null,
                gitRequestEvent = gitRequestEvent,
                isMr = false,
                originYaml = originYaml,
                filePath = existsPipeline.filePath,
                pipelineId = existsPipeline.pipelineId
            ) ?: return false
            val parsedYaml = YamlCommonUtils.toYamlNotNull(objects.preYaml)
            val gitBuildId = gitRequestEventBuildDao.save(
                dslContext = dslContext,
                eventId = gitRequestEvent.id!!,
                originYaml = originYaml!!,
                parsedYaml = parsedYaml,
                normalizedYaml = YamlUtil.toYaml(objects.normalYaml),
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch,
                objectKind = gitRequestEvent.objectKind,
                description = triggerBuildReq.customCommitMsg,
                triggerUser = gitRequestEvent.userId,
                sourceGitProjectId = gitRequestEvent.sourceGitProjectId,
                buildStatus = BuildStatus.RUNNING
            )
            V2GitCIRequestDispatcher.dispatch(
                rabbitTemplate,
                V2GitCIRequestTriggerEvent(
                    pipeline = buildPipeline,
                    event = gitRequestEvent,
                    yaml = objects.normalYaml,
                    parsedYaml = parsedYaml,
                    originYaml = originYaml,
                    normalizedYaml = YamlUtil.toYaml(objects.normalYaml),
                    gitBuildId = gitBuildId
                )
            )
            return true
        }
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
        return checkRequest(gitRequestEvent, event)
    }

    private fun checkRequest(gitRequestEvent: GitRequestEvent, event: GitEvent): Boolean {
        val start = LocalDateTime.now().timestampmilli()
        if (!checkGitProjectConf(gitRequestEvent, event)) return false
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId)
            ?: throw OperationException("git ci projectCode not exist")
        val path2PipelineExists = gitPipelineResourceDao.getAllByGitProjectId(dslContext, gitProjectConf.gitProjectId)
            .map {
                it.filePath to GitProjectPipeline(
                    gitProjectId = it.gitProjectId,
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildInfo = null
                )
            }.toMap()

        // 校验mr请求是否产生冲突
        if (event is GitMergeRequestEvent) {
            return checkMrConflict(gitRequestEvent, event, path2PipelineExists, gitProjectConf)
        }
        return try {
            matchAndTriggerPipeline(gitRequestEvent, event, path2PipelineExists, gitProjectConf)
        } catch (e: Exception) {
            // 触发只要出了异常就把Mr锁定取消，防止出现工蜂项目无法合并
            blockCommitCheck(
                mrEvent = (event is GitMergeRequestEvent),
                event = gitRequestEvent,
                gitProjectConf = gitProjectConf,
                block = false,
                state = GitCICommitCheckState.FAILURE,
                context = noPipelineBuildEvent
            )
            gitCIEventSaveService.saveTriggerNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                reason = TriggerReason.UNKNOWN_ERROR.name,
                reasonDetail = TriggerReason.UNKNOWN_ERROR.detail.format(e.message),
                gitProjectId = gitRequestEvent.gitProjectId
            )
            return false
        } finally {
            logger.info("It takes ${LocalDateTime.now().timestampmilli() - start}ms to match trigger pipeline")
        }
    }

    @Suppress("ALL")
    private fun matchAndTriggerPipeline(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting
    ): Boolean {
        val mrEvent = event is GitMergeRequestEvent
        val hookStartTime = LocalDateTime.now()
        val gitToken = client.getScm(ServiceGitResource::class).getToken(gitRequestEvent.gitProjectId).data!!
        logger.info("get token for gitProject[${gitRequestEvent.gitProjectId}] form scm, token: $gitToken")
        // fork项目库的projectId与原项目不同
        val isFork = isFork(mrEvent, gitRequestEvent)

        var forkGitToken: GitToken? = null
        if (isFork) {
            forkGitToken =
                client.getScm(ServiceGitResource::class).getToken(getProjectId(mrEvent, gitRequestEvent)).data!!
            logger.info(
                "get fork token for gitProject[${
                    getProjectId(
                        mrEvent,
                        gitRequestEvent
                    )
                }] form scm, token: $forkGitToken"
            )
        }

        // 获取指定目录下所有yml文件
        val yamlPathList = if (isFork) {
            getCIYamlList(forkGitToken!!, gitRequestEvent, mrEvent)
        } else {
            getCIYamlList(gitToken, gitRequestEvent, mrEvent)
        }
        // 兼容旧的根目录yml文件
        val isCIYamlExist = if (isFork) {
            isCIYamlExist(forkGitToken!!, gitRequestEvent, mrEvent)
        } else {
            isCIYamlExist(gitToken, gitRequestEvent, mrEvent)
        }
        if (isCIYamlExist) {
            yamlPathList.add(ciFileName)
        }
        logger.info("matchAndTriggerPipeline in gitProjectId:${gitProjectConf.gitProjectId}, yamlPathList: " +
            "$yamlPathList, path2PipelineExists: $path2PipelineExists, " +
            "commitTime:${gitRequestEvent.commitTimeStamp}, " +
            "hookStartTime:${DateTimeUtil.toDateTime(hookStartTime)}, " +
            "yamlCheckedTime:${DateTimeUtil.toDateTime(LocalDateTime.now())}")
        // 如果没有Yaml文件则直接不触发
        if (yamlPathList.isEmpty()) {
            logger.error("gitProjectId: ${gitRequestEvent.gitProjectId} cannot found ci yaml from git")
            gitCIEventSaveService.saveTriggerNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                reason = TriggerReason.CI_YAML_NOT_FOUND.name,
                reasonDetail = TriggerReason.CI_YAML_NOT_FOUND.detail,
                gitProjectId = gitRequestEvent.gitProjectId
            )
            return false
        }

        // mr提交锁定,这时还没有流水线，所以提交的是无流水线锁
        blockCommitCheck(
            mrEvent = mrEvent,
            event = gitRequestEvent,
            gitProjectConf = gitProjectConf,
            context = noPipelineBuildEvent,
            block = true,
            state = GitCICommitCheckState.PENDING
        )

        // 比较Mr请求中的yml版本模拟pre merge，源分支版本落后时对应文件的流水线不触发
        if (mrEvent) {
            val checkMap =
                checkYmlVersion(yamlPathList, gitRequestEvent, forkGitToken?.accessToken, gitToken.accessToken)
            checkMap.forEach { (filePath, isTrigger) ->
                if (!isTrigger) {
                    gitCIEventSaveService.saveBuildNotBuildEvent(
                        userId = gitRequestEvent.userId,
                        eventId = gitRequestEvent.id!!,
                        pipelineId = null,
                        filePath = filePath,
                        originYaml = null,
                        normalizedYaml = null,
                        reason = TriggerReason.CI_YAML_VERSION_BEHIND.name,
                        reasonDetail = TriggerReason.CI_YAML_VERSION_BEHIND.detail,
                        gitProjectId = gitRequestEvent.gitProjectId,
                        sendCommitCheck = true,
                        commitCheckBlock = mrEvent
                    )
                    // 落后版本的文件不触发
                    yamlPathList.remove(filePath)
                }
            }
        }

        yamlPathList.forEach { filePath ->
            // 因为要为 GIT_CI_YAML_INVALID 这个异常添加文件信息，所以先创建流水线，后面再根据Yaml修改流水线名称即可
            var displayName = filePath
            ciFileExtensions.forEach {
                displayName = filePath.removeSuffix(it)
            }
            val existsPipeline = path2PipelineExists[filePath]
            // 如果该流水线已保存过，则继续使用
            val buildPipeline = existsPipeline
                ?: GitProjectPipeline(
                    gitProjectId = gitProjectConf.gitProjectId,
                    displayName = displayName,
                    pipelineId = "", // 留空用于是否创建判断
                    filePath = filePath,
                    enabled = true,
                    creator = gitRequestEvent.userId,
                    latestBuildInfo = null
                )
            var originYaml: String? = null
            try {
                // 为已存在的流水线设置名称
                buildPipeline.displayName = displayName
                originYaml = if (isFork) getYamlFromGit(forkGitToken!!, gitRequestEvent, filePath, mrEvent)
                else getYamlFromGit(gitToken, gitRequestEvent, filePath, mrEvent)
                logger.info("origin yamlStr: $originYaml")

                // 如果当前文件没有内容直接不触发
                if (originYaml.isNullOrBlank()) {
                    logger.warn(
                        "Matcher is false, return, " +
                                "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
                    )
                    gitCIEventSaveService.saveBuildNotBuildEvent(
                        userId = gitRequestEvent.userId,
                        eventId = gitRequestEvent.id!!,
                        pipelineId = buildPipeline.pipelineId.ifBlank { null },
                        filePath = buildPipeline.filePath,
                        originYaml = originYaml,
                        normalizedYaml = null,
                        reason = TriggerReason.CI_YAML_CONTENT_NULL.name,
                        reasonDetail = TriggerReason.CI_YAML_CONTENT_NULL.detail,
                        gitProjectId = gitRequestEvent.gitProjectId,
                        sendCommitCheck = true,
                        commitCheckBlock = mrEvent
                    )
                    return@forEach
                }

                // 流水线未启用则跳过
                if (!buildPipeline.enabled) {
                    logger.warn(
                        "Pipeline is not enabled, return, " +
                                "gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
                    )
                    gitCIEventSaveService.saveBuildNotBuildEvent(
                        userId = gitRequestEvent.userId,
                        eventId = gitRequestEvent.id!!,
                        pipelineId = buildPipeline.pipelineId,
                        filePath = buildPipeline.filePath,
                        originYaml = originYaml,
                        normalizedYaml = null,
                        reason = TriggerReason.PIPELINE_DISABLE.name,
                        reasonDetail = TriggerReason.PIPELINE_DISABLE.detail,
                        gitProjectId = gitRequestEvent.gitProjectId,
                        sendCommitCheck = false,
                        commitCheckBlock = false
                    )
                    return@forEach
                }

                // 检查yml版本，根据yml版本选择不同的实现
                val ymlVersion = ScriptYmlUtils.parseVersion(originYaml)
                val triggerInterface = requestTriggerFactory.getGitCIRequestTrigger(ymlVersion)
                if (!triggerInterface.triggerBuild(
                        gitToken = gitToken,
                        forkGitToken = forkGitToken,
                        gitRequestEvent = gitRequestEvent,
                        gitProjectPipeline = buildPipeline,
                        event = event,
                        originYaml = originYaml,
                        filePath = filePath
                    )
                ) {
                    return@forEach
                }
            } catch (e: Exception) {
                logger.error(
                    "yamlPathList in gitProjectId:${gitProjectConf.gitProjectId} has invalid yaml file[$filePath]: ",
                    e
                )
                gitCIEventSaveService.saveBuildNotBuildEvent(
                    userId = gitRequestEvent.userId,
                    eventId = gitRequestEvent.id!!,
                    pipelineId = buildPipeline.pipelineId,
                    filePath = buildPipeline.filePath,
                    originYaml = originYaml,
                    normalizedYaml = null,
                    reason = TriggerReason.CI_YAML_INVALID.name,
                    reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message),
                    gitProjectId = gitRequestEvent.gitProjectId,
                    sendCommitCheck = true,
                    commitCheckBlock = mrEvent
                )
                return@forEach
            }
        }
        // yml校验全部结束后，解除锁定
        logger.info("")
        blockCommitCheck(
            mrEvent = mrEvent,
            event = gitRequestEvent,
            gitProjectConf = gitProjectConf,
            context = noPipelineBuildEvent,
            block = false,
            state = GitCICommitCheckState.SUCCESS
        )
        return true
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
                    ?: throw CustomException(Response.Status.INTERNAL_SERVER_ERROR,
                        "Git CI没有此镜像版本记录. ${it.image}")
                if (!record.enable) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "镜像版本不可用. ${it.image}")
                }
            }
        }

        return CiYamlUtils.normalizeGitCiYaml(yamlObject)
    }

    private fun prepareCIBuildYaml(
        gitRequestEvent: GitRequestEvent,
        originYaml: String?,
        filePath: String,
        pipelineId: String?
    ): Pair<CIBuildYaml, String>? {

        if (originYaml.isNullOrBlank()) {
            return null
        }

        val yamlObject = try {
            createCIBuildYaml(originYaml!!, gitRequestEvent.gitProjectId)
        } catch (e: Throwable) {
            logger.error("git ci yaml is invalid", e)
            // 手动触发不发送commitCheck
            gitCIEventSaveService.saveBuildNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                pipelineId = pipelineId,
                filePath = filePath,
                originYaml = originYaml,
                normalizedYaml = null,
                reason = TriggerReason.CI_YAML_INVALID.name,
                reasonDetail = TriggerReason.CI_YAML_INVALID.detail.format(e.message),
                gitProjectId = gitRequestEvent.gitProjectId,
                sendCommitCheck = false,
                commitCheckBlock = false
            )
            return null
        }

        val normalizedYaml = YamlUtil.toYaml(yamlObject)
        logger.info("normalize yaml: $normalizedYaml")
        return Pair(yamlObject, normalizedYaml)
    }

    private fun checkGitProjectConf(gitRequestEvent: GitRequestEvent, event: GitEvent): Boolean {
        val gitProjectSetting = gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId)
        // 完全没创建过得项目不存记录
        if (null == gitProjectSetting) {
            logger.info("git ci is not enabled, git project id: ${gitRequestEvent.gitProjectId}")
            return false
        }
        if (!gitProjectSetting.enableCi) {
            logger.warn("git ci is disabled, git project id: ${gitRequestEvent.gitProjectId}, " +
                "name: ${gitProjectSetting.name}")
            gitCIEventSaveService.saveTriggerNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                reason = TriggerReason.CI_DISABLED.name,
                reasonDetail = TriggerReason.CI_DISABLED.detail,
                gitProjectId = gitRequestEvent.gitProjectId
            )
            return false
        }
        when (event) {
            is GitPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn("git ci conf buildPushedBranches is false, git project id: " +
                        "${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    gitCIEventSaveService.saveTriggerNotBuildEvent(
                        userId = gitRequestEvent.userId,
                        eventId = gitRequestEvent.id!!,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED.name,
                        reasonDetail = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED.detail,
                        gitProjectId = gitRequestEvent.gitProjectId
                    )
                    return false
                }
            }
            is GitTagPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn("git ci conf buildPushedBranches is false, git project id: " +
                        "${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    gitCIEventSaveService.saveTriggerNotBuildEvent(
                        userId = gitRequestEvent.userId,
                        eventId = gitRequestEvent.id!!,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED.name,
                        reasonDetail = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED.detail,
                        gitProjectId = gitRequestEvent.gitProjectId
                    )
                    return false
                }
            }
            is GitMergeRequestEvent -> {
                if (!gitProjectSetting.buildPushedPullRequest) {
                    logger.warn("git ci conf buildMergePullRequest is false, git project id: " +
                        "${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    gitCIEventSaveService.saveTriggerNotBuildEvent(
                        userId = gitRequestEvent.userId,
                        eventId = gitRequestEvent.id!!,
                        reason = TriggerReason.BUILD_MERGE_REQUEST_DISABLED.name,
                        reasonDetail = TriggerReason.BUILD_MERGE_REQUEST_DISABLED.detail,
                        gitProjectId = gitRequestEvent.gitProjectId
                    )
                    return false
                }
            }
        }
        return true
    }

    /**
     * 检查请求中是否有冲突
     * - 冲突通过请求详情获取，冲突检查为异步，需要通过延时队列轮训冲突检查结果
     * - 有冲突，不触发
     * - 没有冲突，进行后续操作
     */
    private fun checkMrConflict(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting
    ): Boolean {
        val gitToken = client.getScm(ServiceGitResource::class).getToken(gitRequestEvent.gitProjectId).data!!
        logger.info("get token form scm, token: $gitToken")

        val projectId = gitRequestEvent.gitProjectId
        val mrRequestId = (event as GitMergeRequestEvent).object_attributes.id

        val mrInfo = client.getScm(ServiceGitResource::class).getGitCIMrInfo(
            gitProjectId = projectId,
            mergeRequestId = mrRequestId,
            token = gitToken.accessToken
        ).data!!
        // 通过查询当前merge请求的状态，unchecked说明未检查完，进入延迟队列
        when (mrInfo.mergeStatus) {
            GitCiMergeStatus.MERGE_STATUS_UNCHECKED.value -> {
                // 第一次未检查完则改变状态为正在检查供用户查看
                val recordId = gitCIEventSaveService.saveTriggerNotBuildEvent(
                    userId = gitRequestEvent.userId,
                    eventId = gitRequestEvent.id!!,
                    reason = TriggerReason.CI_MERGE_CHECKING.name,
                    reasonDetail = TriggerReason.CI_MERGE_CHECKING.detail,
                    gitProjectId = gitRequestEvent.gitProjectId
                )

                dispatchMrConflictCheck(
                    GitCIMrConflictCheckEvent(
                        token = gitToken.accessToken,
                        gitRequestEvent = gitRequestEvent,
                        event = event,
                        path2PipelineExists = path2PipelineExists,
                        gitProjectConf = gitProjectConf,
                        notBuildRecordId = recordId
                    )
                )
                return true
            }
            GitCiMergeStatus.MERGE_STATUS_CAN_NOT_BE_MERGED.value -> {
                logger.warn("git ci mr request has conflict , git project id: $projectId, mr request id: $mrRequestId")
                gitCIEventSaveService.saveTriggerNotBuildEvent(
                    userId = gitRequestEvent.userId,
                    eventId = gitRequestEvent.id!!,
                    reason = TriggerReason.CI_MERGE_CONFLICT.name,
                    reasonDetail = TriggerReason.CI_MERGE_CONFLICT.detail,
                    gitProjectId = gitRequestEvent.gitProjectId
                )
                return false
            }
            // 没有冲突则触发流水线
            else -> return matchAndTriggerPipeline(
                gitRequestEvent = gitRequestEvent,
                event = event,
                path2PipelineExists = path2PipelineExists,
                gitProjectConf = gitProjectConf
            )
        }
    }

    // 检查是否存在冲突，供Rabbit Listener使用
    fun checkMrConflictByListener(
        token: String,
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting,
        // 是否是最后一次的检查
        isEndCheck: Boolean = false,
        notBuildRecordId: Long
    ): Boolean {
        val projectId = gitRequestEvent.gitProjectId
        val mrRequestId = (event as GitMergeRequestEvent).object_attributes.id

        val mrInfo = client.getScm(ServiceGitResource::class).getGitCIMrInfo(
            gitProjectId = projectId,
            mergeRequestId = mrRequestId,
            token = token
        ).data!!
        when (mrInfo.mergeStatus) {
            GitCiMergeStatus.MERGE_STATUS_UNCHECKED.value -> {
                // 如果最后一次检查还未检查完就是检查超时
                if (isEndCheck) {
                    // 第一次之后已经在not build中有数据了，修改构建原因
                    gitRequestEventNotBuildDao.updateNoBuildReasonByRecordId(
                        dslContext = dslContext,
                        recordId = notBuildRecordId,
                        reason = TriggerReason.CI_MERGE_CHECK_TIMEOUT.name,
                        reasonDetail = TriggerReason.CI_MERGE_CHECK_TIMEOUT.detail
                    )
                }
                return false
            }
            GitCiMergeStatus.MERGE_STATUS_CAN_NOT_BE_MERGED.value -> {
                logger.warn("git ci mr request has conflict , git project id: $projectId, mr request id: $mrRequestId")
                gitRequestEventNotBuildDao.updateNoBuildReasonByRecordId(
                    dslContext = dslContext,
                    recordId = notBuildRecordId,
                    reason = TriggerReason.CI_MERGE_CONFLICT.name,
                    reasonDetail = TriggerReason.CI_MERGE_CONFLICT.detail
                )
                return true
            }
            else -> {
                gitRequestEventNotBuildDao.deleteNoBuildsById(
                    dslContext = dslContext,
                    recordId = notBuildRecordId
                )
                matchAndTriggerPipeline(
                    gitRequestEvent = gitRequestEvent,
                    event = event,
                    path2PipelineExists = path2PipelineExists,
                    gitProjectConf = gitProjectConf
                )
                return true
            }
        }
    }

    /**
     * MR触发时，yml以谁为准：
     * - 源和目标的配置文件做对比，未变更取源分支。(取源分支的文件列表做遍历)
     * - 有变更时，判断源分支和目标分支的版本新旧：
     *   - 源分支新（目标分支的最后一次提交在源分支中找得到）触发，取源分支版本
     *   - 目标分支新，对应文件的流水线不触发，报错并说明原因
     * 注：注意存在fork库不同projectID的提交
     */
    private fun checkYmlVersion(
        yamlPathList: List<String>,
        gitRequestEvent: GitRequestEvent,
        forkToken: String?,
        token: String
    ): Map<String, Boolean> {
        val targetProjectId = gitRequestEvent.gitProjectId
        val checkMap = mutableMapOf<String, Boolean>()
        yamlPathList.forEach {
            val commits = client.getScm(ServiceGitResource::class).getCommits(
                gitProjectId = targetProjectId,
                filePath = it,
                branch = gitRequestEvent.targetBranch!!,
                token = token,
                page = 1,
                perPage = 1,
                since = null,
                until = null
            ).data!!
            // 目标分支找不到说明是新文件，默认为源分支版本新
            if (commits.isEmpty()) {
                checkMap[it] = true
                return@forEach
            }
            // 找得到的，对比当前文件在目标分支的最后一次提交在源分支是否可以找到
            val lastCommit = commits.first()
            // fork 库的token不同
            val sourceToken = forkToken ?: token
            // 通过时间区分来减少搜索范围
            // 目前暂定为100条后续看实际情况判断是否添加分页拉取
            val sourceCommits = client.getScm(ServiceGitResource::class).getCommits(
                gitProjectId = getProjectId(true, gitRequestEvent),
                filePath = it,
                branch = gitRequestEvent.branch,
                token = sourceToken,
                page = 1,
                perPage = 100,
                since = lastCommit.committedDate,
                until = lastCommit.committedDate
            ).data!!
            // 没有提交记录说明目标分支比较新，源分支版本落后
            if (sourceCommits.isEmpty()) {
                checkMap[it] = false
                return@forEach
            } else {
                val sourceCommitSet = sourceCommits.map { commit -> commit.id }.toSet()
                // 在源分支中没有包含这次提交，说明源分支版本落后
                if (lastCommit.id !in sourceCommitSet) {
                    checkMap[it] = false
                    return@forEach
                }
            }
            checkMap[it] = true
        }
        return checkMap
    }

    private fun isFork(isMrEvent: Boolean, gitRequestEvent: GitRequestEvent): Boolean {
        return isMrEvent && gitRequestEvent.sourceGitProjectId != null && gitRequestEvent.sourceGitProjectId !=
                gitRequestEvent.gitProjectId
    }

    // mr锁定提交
    private fun blockCommitCheck(
        mrEvent: Boolean,
        event: GitRequestEvent,
        gitProjectConf: GitCIBasicSetting,
        block: Boolean,
        state: GitCICommitCheckState,
        context: String
    ) {
        logger.info("CommitCheck with block, gitProjectId:${event.gitProjectId}, mrEvent:$mrEvent, " +
            "block:$block, state:$state, context:$context, enableMrBlock:${gitProjectConf.enableMrBlock}")
//        if (!isMrEvent) {
//            // push事件也发送commitCheck不加锁
//            scmClient.pushCommitCheckWithBlock(
//                commitId = event.commitId,
//                mergeRequestId = event.mergeRequestId ?: 0L,
//                userId = event.userId,
//                block = false,
//                state = state,
//                context = context,
//                gitCIBasicSetting = gitProjectConf
//            )
//        }
        if (gitProjectConf.enableMrBlock && mrEvent) {
            scmClient.pushCommitCheckWithBlock(
                commitId = event.commitId,
                mergeRequestId = event.mergeRequestId ?: 0L,
                userId = event.userId,
                block = block,
                state = state,
                context = context,
                gitCIBasicSetting = gitProjectConf
            )
        }
    }

    private fun replaceEnv(yaml: String, gitProjectId: Long?): String {
        if (gitProjectId == null) {
            return yaml
        }
        val gitProjectConf = gitCIV1SettingDao.getSetting(dslContext, gitProjectId) ?: return yaml
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

    private fun dispatchMrConflictCheck(event: GitCIMrConflictCheckEvent) {
        GitCIMrConflictCheckDispatcher.dispatch(rabbitTemplate, event)
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

    private fun getCIYamlList(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        isMrEvent: Boolean = false
    ): MutableList<String> {
        val ciFileList = getFileTreeFromGit(gitToken, gitRequestEvent, ciFileDirectoryName, isMrEvent)
            .filter { it.name.endsWith(ciFileExtensionYml) || it.name.endsWith(ciFileExtensionYaml) }
        return ciFileList.map { ciFileDirectoryName + File.separator + it.name }.toMutableList()
    }

    private fun isCIYamlExist(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        isMrEvent: Boolean = false
    ): Boolean {
        val ciFileList = getFileTreeFromGit(gitToken, gitRequestEvent, "", isMrEvent)
            .filter { it.name == ciFileName }
        return ciFileList.isNotEmpty()
    }

    private fun getFileTreeFromGit(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        filePath: String,
        isMrEvent: Boolean = false
    ): List<GitFileInfo> {
        return try {
            val result = client.getScm(ServiceGitResource::class).getGitCIFileTree(
                gitProjectId = getProjectId(isMrEvent, gitRequestEvent),
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

    // 获取项目ID，兼容没有source字段的旧数据，和fork库中源项目id不同的情况
    private fun getProjectId(isMrEvent: Boolean = false, gitRequestEvent: GitRequestEvent): Long {
        with(gitRequestEvent) {
            return if (isMrEvent && sourceGitProjectId != null && sourceGitProjectId != gitProjectId) {
                sourceGitProjectId!!
            } else {
                gitProjectId
            }
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
                    (event.object_attributes.action == "update" &&
                        event.object_attributes.extension_action != "push-update")
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
            sourceGitProjectId = null,
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
            sourceGitProjectId = null,
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
            gitProjectId = gitMrEvent.object_attributes.target_project_id,
            sourceGitProjectId = gitMrEvent.object_attributes.source_project_id,
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
            sourceGitProjectId = null,
            branch = getBranchName(triggerBuildReq.branch),
            targetBranch = null,
            commitId = triggerBuildReq.commitId ?: "",
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

    private fun getTriggerBranch(gitRequestEvent: GitRequestEvent): String {
        return when {
            gitRequestEvent.branch.startsWith("refs/heads/") ->
                gitRequestEvent.branch.removePrefix("refs/heads/")
            gitRequestEvent.branch.startsWith("refs/tags/") ->
                gitRequestEvent.branch.removePrefix("refs/tags/")
            else -> gitRequestEvent.branch
        }
//        return if (gitRequestEvent.objectKind == OBJECT_KIND_MERGE_REQUEST) {
//            when {
//                gitRequestEvent.targetBranch!!.startsWith("refs/heads/")
    //                -> gitRequestEvent.targetBranch!!.removePrefix("refs/heads/")
//                gitRequestEvent.targetBranch!!.startsWith("refs/tags/")
    //                -> gitRequestEvent.targetBranch!!.removePrefix("refs/tags/")
//                else -> gitRequestEvent.targetBranch!!
//            }
//        } else {
//            when {
//                gitRequestEvent.branch.startsWith("refs/heads/") ->
    //                gitRequestEvent.branch.removePrefix("refs/heads/")
//                gitRequestEvent.branch.startsWith("refs/tags/") ->
    //                gitRequestEvent.branch.removePrefix("refs/tags/")
//                else -> gitRequestEvent.branch
//            }
//        }
    }

    private fun getBranchName(ref: String): String {
        return when {
            ref.startsWith("refs/heads/") ->
                ref.removePrefix("refs/heads/")
            ref.startsWith("refs/tags/") ->
                ref.removePrefix("refs/tags/")
            else -> ref
        }
    }

    fun getYaml(gitProjectId: Long, buildId: String): String {
        logger.info("get yaml by buildId:($buildId), gitProjectId: $gitProjectId")
        gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId)
        return (eventBuild?.originYaml) ?: ""
    }

    fun getYamlV2(gitProjectId: Long, buildId: String): V2BuildYaml? {
        logger.info("get yaml by buildId:($buildId), gitProjectId: $gitProjectId")
        gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启工蜂CI，无法查询"
        )
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        // 针对V2版本做替换
        val parsed = eventBuild.parsedYaml.replaceFirst("triggerOn:", "on:")
        return V2BuildYaml(parsedYaml = parsed, originYaml = eventBuild.originYaml)
    }
}
