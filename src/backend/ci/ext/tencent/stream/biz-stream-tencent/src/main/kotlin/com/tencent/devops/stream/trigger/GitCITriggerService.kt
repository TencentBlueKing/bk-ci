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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.stream.client.ScmClient
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.TriggerException.Companion.triggerError
import com.tencent.devops.stream.common.exception.TriggerThirdException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.mq.streamTrigger.StreamTriggerDispatch
import com.tencent.devops.stream.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.git.GitEvent
import com.tencent.devops.stream.pojo.git.GitMergeRequestEvent
import com.tencent.devops.stream.pojo.git.GitPushEvent
import com.tencent.devops.stream.pojo.git.GitTagPushEvent
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.stream.v2.service.StreamScmService
import com.tencent.devops.stream.v2.service.GitPipelineBranchService
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.scm.pojo.GitCodeFileInfo
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitMergeActionKind
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.triggerParameter.TriggerParameter
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.util.Base64

@Suppress("ComplexCondition")
@Service
class GitCITriggerService @Autowired constructor(
    private val client: Client,
    private val scmClient: ScmClient,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val gitCISettingDao: GitCIBasicSettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val rabbitTemplate: RabbitTemplate,
    private val yamlTriggerFactory: YamlTriggerFactory,
    private val gitCIEventSaveService: GitCIEventService,
    private val gitPipelineBranchService: GitPipelineBranchService,
    private val streamScmService: StreamScmService,
    private val triggerParameter: TriggerParameter,
    private val mergeConflictCheck: MergeConflictCheck,
    private val triggerExceptionService: TriggerExceptionService,
    private val tokenService: StreamGitTokenService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerService::class.java)
        private val channelCode = ChannelCode.GIT
        private val ciFileExtensions = listOf(".yml", ".yaml")
        private const val ciFileExtensionYml = ".yml"
        private const val ciFileExtensionYaml = ".yaml"
        private const val ciFileName = ".ci.yml"
        private const val ciFileDirectoryName = ".ci"
        const val noPipelineBuildEvent = "MR held, waiting until pipeline validation finish."
    }

    fun externalCodeGitBuild(event: String): Boolean? {
        logger.info("Trigger code git build($event)")

        val eventObject = try {
            objectMapper.readValue<GitEvent>(event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
            return false
        }

        val gitRequestEvent = triggerParameter.saveGitRequestEvent(eventObject, event) ?: return true

        val gitCIBasicSetting = gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId)
        // 完全没创建过得项目不存记录
        if (null == gitCIBasicSetting) {
            logger.info("git ci is not enabled, git project id: ${gitRequestEvent.gitProjectId}")
            return null
        }

        return triggerExceptionService.handle(gitRequestEvent, eventObject, gitCIBasicSetting) {
            checkRequest(gitRequestEvent, eventObject, gitCIBasicSetting)
        }
    }

    private fun checkRequest(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        gitProjectConf: GitCIBasicSetting
    ): Boolean {
        val start = LocalDateTime.now().timestampmilli()

        checkGitProjectConf(gitRequestEvent, event, gitProjectConf)

        val path2PipelineExists = gitPipelineResourceDao.getAllByGitProjectId(dslContext, gitProjectConf.gitProjectId)
            .associate {
                it.filePath to GitProjectPipeline(
                    gitProjectId = it.gitProjectId,
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildInfo = null
                )
            }

        // 校验mr请求是否产生冲突，已合并的无需检查
        if (event is GitMergeRequestEvent &&
            event.object_attributes.action != TGitMergeActionKind.MERGE.value &&
            !mergeConflictCheck.checkMrConflict(
                gitRequestEvent = gitRequestEvent,
                event = event,
                path2PipelineExists = path2PipelineExists,
                gitProjectConf = gitProjectConf,
                gitToken = handleGetToken(gitRequestEvent)!!
            )
        ) {
            return false
        }
        logger.info("It takes ${LocalDateTime.now().timestampmilli() - start}ms to match trigger pipeline")

        return matchAndTriggerPipeline(gitRequestEvent, event, path2PipelineExists, gitProjectConf)
    }

    @Suppress("ALL")
    fun matchAndTriggerPipeline(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting
    ): Boolean {
        val mrEvent = event is GitMergeRequestEvent
        val isMerged = if (mrEvent) {
            val e = event as GitMergeRequestEvent
            e.object_attributes.action == TGitMergeActionKind.MERGE.value
        } else {
            false
        }
        val hookStartTime = LocalDateTime.now()
        val gitToken = handleGetToken(gitRequestEvent)!!
        logger.info("get token for gitProject[${gitRequestEvent.gitProjectId}] form scm, token: $gitToken")

        // fork项目库的projectId与原项目不同
        val isFork = !isMerged && isFork(mrEvent, gitRequestEvent)
        var forkGitToken: String? = null
        if (isFork) {
            forkGitToken = handleGetToken(gitRequestEvent, true)!!
            logger.info(
                "get fork token for gitProject[${
                    getProjectId(
                        mrEvent,
                        gitRequestEvent
                    )
                }] form scm, token: $forkGitToken"
            )
        }

        // 判断本次push提交是否需要删除流水线
        if (event is GitPushEvent) {
            checkAndDeletePipeline(gitRequestEvent, event, path2PipelineExists, gitProjectConf)
        }

        // 获取指定目录下所有yml文件
        val yamlPathList = if (isFork) {
            getCIYamlList(forkGitToken!!, gitRequestEvent, mrEvent)
        } else {
            getCIYamlList(gitToken, gitRequestEvent, mrEvent)
        }.toMutableList()
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
            logger.warn("event: ${gitRequestEvent.id} cannot found ci yaml from git")
            triggerError(
                request = gitRequestEvent,
                reason = TriggerReason.CI_YAML_NOT_FOUND
            )
        }

        // mr提交锁定,这时还没有流水线，所以提交的是无流水线锁
        blockCommitCheck(
            mrEvent = mrEvent,
            event = gitRequestEvent,
            gitProjectConf = gitProjectConf,
            block = true,
            state = GitCICommitCheckState.PENDING
        )

        // 获取mr请求的变更文件列表，用来给后面判断，Merged事件不用检查版本
        val changeSet = if (mrEvent && !isMerged) {
            // 由于前面提交无流水线锁，所以这个出错需要解锁
            triggerExceptionService.handleErrorCode(
                request = gitRequestEvent,
                commitCheck = CommitCheck(
                    isNoPipelineCheck = true,
                    block = false,
                    state = GitCICommitCheckState.FAILURE
                ),
                action = {
                    streamScmService.getMergeRequestChangeInfo(
                        userId = null,
                        token = gitToken,
                        gitProjectId = gitRequestEvent.gitProjectId,
                        mrId = (event as GitMergeRequestEvent).object_attributes.id
                    )
                }
            )?.files?.filter { !it.deletedFile }?.map { it.newPath }?.toSet() ?: emptySet()
        } else {
            emptySet()
        }

        yamlPathList.forEach { filePath ->

            // 因为要为 GIT_CI_YAML_INVALID 这个异常添加文件信息，所以先创建流水线，后面再根据Yaml修改流水线名称即可
            var displayName = filePath
            ciFileExtensions.forEach {
                displayName = filePath.removeSuffix(it)
            }
            val existsPipeline = path2PipelineExists[filePath]
            // 如果该流水线已保存过，则继续使用
            val buildPipeline = if (existsPipeline != null) {
                // mr请求不涉及删除操作
                if (!mrEvent) {
                    // 触发时新增流水线-分支记录
                    gitPipelineBranchService.save(
                        gitProjectId = gitProjectConf.gitProjectId,
                        pipelineId = existsPipeline.pipelineId,
                        branch = gitRequestEvent.branch
                    )
                }
                existsPipeline
            } else {
                // 对于来自fork库的mr新建的流水线，当前库不维护其状态
                GitProjectPipeline(
                    gitProjectId = gitProjectConf.gitProjectId,
                    displayName = displayName,
                    pipelineId = "", // 留空用于是否创建判断
                    filePath = filePath,
                    enabled = true,
                    creator = gitRequestEvent.userId,
                    latestBuildInfo = null
                )
            }
            // 针对每个流水线处理异常
            triggerExceptionService.handle(gitRequestEvent, event, gitProjectConf) {
                // ErrorCode都是系统错误，在最外面统一处理,都要发送无锁的commitCheck
                triggerExceptionService.handleErrorCode(
                    request = gitRequestEvent,
                    event = event,
                    pipeline = buildPipeline,
                    action = {
                        checkAndTrigger(
                            buildPipeline = buildPipeline,
                            gitRequestEvent = gitRequestEvent,
                            event = event,
                            forkGitToken = forkGitToken,
                            gitToken = gitToken,
                            changeSet = changeSet,
                            displayName = displayName,
                            mrEvent = mrEvent,
                            isMerged = isMerged,
                            gitProjectConf = gitProjectConf,
                            forkGitProjectId = null
                        )
                    },
                    commitCheck = CommitCheck(
                        block = false,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            }
        }
        // yml校验全部结束后，解除锁定
        blockCommitCheck(
            mrEvent = mrEvent,
            event = gitRequestEvent,
            gitProjectConf = gitProjectConf,
            block = false,
            state = GitCICommitCheckState.SUCCESS
        )
        return true
    }

    @Throws(ErrorCodeException::class)
    private fun checkAndTrigger(
        buildPipeline: GitProjectPipeline,
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        forkGitToken: String?,
        gitToken: String,
        changeSet: Set<String>,
        displayName: String,
        mrEvent: Boolean,
        isMerged: Boolean,
        gitProjectConf: GitCIBasicSetting,
        forkGitProjectId: Long?
    ) {
        val filePath = buildPipeline.filePath
        // 流水线未启用则跳过
        if (!buildPipeline.enabled) {
            logger.warn(
                "Pipeline is not enabled, gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
            )
            triggerError(
                request = gitRequestEvent,
                event = event,
                pipeline = buildPipeline,
                reason = TriggerReason.PIPELINE_DISABLE
            )
        }

        // 检查版本落后信息和真正要触发的文件，Merged事件不用检查版本
        val originYaml = if (mrEvent && !isMerged) {
            val (result, orgYaml) =
                checkYmlVersion(
                    mrEvent = event as GitMergeRequestEvent,
                    sourceGitToken = forkGitToken,
                    targetGitToken = gitToken,
                    filePath = filePath,
                    changeSet = changeSet
                )

            logger.info("origin yamlStr: $orgYaml")
            if (!result) {
                triggerError(
                    request = gitRequestEvent,
                    event = event,
                    pipeline = buildPipeline,
                    reason = TriggerReason.CI_YAML_NEED_MERGE_OR_REBASE,
                    commitCheck = CommitCheck(
                        block = mrEvent,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            }
            orgYaml
        } else {
            streamScmService.getYamlFromGit(
                token = forkGitToken ?: gitToken,
                ref = gitRequestEvent.branch,
                fileName = filePath,
                gitProjectId = getProjectId(mrEvent, gitRequestEvent).toString(),
                useAccessToken = true
            )
        }

        // 为已存在的流水线设置名称
        buildPipeline.displayName = displayName

        // 如果当前文件没有内容直接不触发
        if (originYaml.isBlank()) {
            logger.warn(
                "Matcher is false,gitProjectId: ${gitRequestEvent.gitProjectId}, eventId: ${gitRequestEvent.id}"
            )
            triggerError(
                request = gitRequestEvent,
                event = event,
                pipeline = buildPipeline,
                reason = TriggerReason.CI_YAML_CONTENT_NULL,
                yamls = Yamls(
                    originYaml = originYaml,
                    parsedYaml = null,
                    normalYaml = null
                ),
                commitCheck = CommitCheck(
                    block = mrEvent,
                    state = GitCICommitCheckState.FAILURE
                )
            )
        }

        // 检查yml版本，根据yml版本选择不同的实现
        val ymlVersion = ScriptYmlUtils.parseVersion(originYaml)
        val triggerInterface = yamlTriggerFactory.getGitCIRequestTrigger(ymlVersion)
        if (ymlVersion?.version == "v2.0") {
            dispatchStreamTrigger(
                StreamTriggerEvent(
                    gitRequestEvent = gitRequestEvent,
                    gitProjectPipeline = buildPipeline,
                    event = event,
                    originYaml = originYaml,
                    filePath = filePath,
                    gitCIBasicSetting = gitProjectConf,
                    forkGitProjectId = forkGitProjectId,
                    // TODO 为了保证消息生产消费兼容，下次发布再去掉event的token字段
                    gitToken = GitToken(accessToken = gitToken),
                    forkGitToken = GitToken(accessToken = forkGitToken ?: "")
                )
            )
        } else {
            triggerInterface.triggerBuild(
                gitRequestEvent = gitRequestEvent,
                gitProjectPipeline = buildPipeline,
                event = event,
                originYaml = originYaml,
                filePath = filePath,
                forkGitProjectId = forkGitProjectId
            )
        }
    }

    private fun dispatchStreamTrigger(event: StreamTriggerEvent) {
        StreamTriggerDispatch.dispatch(rabbitTemplate, event)
    }

    @Throws(TriggerException::class)
    private fun checkGitProjectConf(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        gitProjectSetting: GitCIBasicSetting
    ): Boolean {
        if (!gitProjectSetting.enableCi) {
            logger.warn("git ci is disabled, git project id: ${gitRequestEvent.gitProjectId}, " +
                "name: ${gitProjectSetting.name}")
            triggerError(
                request = gitRequestEvent,
                reason = TriggerReason.CI_DISABLED
            )
        }
        when (event) {
            is GitPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn("git ci conf buildPushedBranches is false, git project id: " +
                        "${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    triggerError(
                        request = gitRequestEvent,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED
                    )
                }
            }
            is GitTagPushEvent -> {
                if (!gitProjectSetting.buildPushedBranches) {
                    logger.warn("git ci conf buildPushedBranches is false, git project id: " +
                        "${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    triggerError(
                        request = gitRequestEvent,
                        reason = TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED
                    )
                }
            }
            is GitMergeRequestEvent -> {
                if (!gitProjectSetting.buildPushedPullRequest) {
                    logger.warn("git ci conf buildMergePullRequest is false, git project id: " +
                        "${gitRequestEvent.gitProjectId}, name: ${gitProjectSetting.name}")
                    triggerError(
                        request = gitRequestEvent,
                        reason = TriggerReason.BUILD_MERGE_REQUEST_DISABLED
                    )
                }
            }
        }
        return true
    }

    /**
     * MR触发时，yml以谁为准：
     * - 当前MR变更中不存在yml文件，取目标分支（默认为未改动时目标分支永远是最新的）
     * - 当前MR变更中存在yml文件，通过对比两个文件的blobId：
     *   - blobId一样/目标分支文件不存，取源分支文件
     *   - blobId不一样，判断当前文件的根提交的blobID是否相同
     *      - 如果相同取源分支的(更新过了)
     *      - 如果不同，报错提示用户yml文件版本落后需要更新
     * 注：注意存在fork库不同projectID的提交
     */
    @Throws(ErrorCodeException::class)
    private fun checkYmlVersion(
        mrEvent: GitMergeRequestEvent,
        targetGitToken: String,
        sourceGitToken: String?,
        filePath: String,
        changeSet: Set<String>
    ): Pair<Boolean, String> {
        val targetFile = getFileInfo(
            token = targetGitToken,
            ref = mrEvent.object_attributes.target_branch,
            filePath = filePath,
            gitProjectId = mrEvent.object_attributes.target_project_id.toString()
        )
        if (!changeSet.contains(filePath)) {
            return if (targetFile?.content.isNullOrBlank()) {
                Pair(true, "")
            } else {
                Pair(true, String(Base64.getDecoder().decode(targetFile!!.content)))
            }
        }

        val sourceFile = getFileInfo(
            token = sourceGitToken ?: targetGitToken,
            ref = mrEvent.object_attributes.source_branch,
            filePath = filePath,
            gitProjectId = mrEvent.object_attributes.source_project_id.toString()
        )
        val sourceContent = if (sourceFile?.content.isNullOrBlank()) {
            ""
        } else {
            String(Base64.getDecoder().decode(sourceFile!!.content))
        }

        if (targetFile?.blobId.isNullOrBlank()) {
            return Pair(true, sourceContent)
        }

        if (targetFile?.blobId == sourceFile?.blobId) {
            return Pair(true, sourceContent)
        }

        val mergeRequest = streamScmService.getMergeInfo(
            gitProjectId = mrEvent.object_attributes.target_project_id,
            mergeRequestId = mrEvent.object_attributes.id,
            token = targetGitToken
        )
        val baseTargetFile = getFileInfo(
            token = targetGitToken,
            ref = mergeRequest.baseCommit,
            filePath = filePath,
            gitProjectId = mrEvent.object_attributes.target_project_id.toString()
        )
        if (targetFile?.blobId == baseTargetFile?.blobId) {
            return Pair(true, sourceContent)
        }

        return Pair(false, "")
    }

    private fun getFileInfo(
        token: String,
        gitProjectId: String,
        filePath: String?,
        ref: String?
    ): GitCodeFileInfo? {
        return try {
            streamScmService.getFileInfo(
                token = token,
                ref = ref,
                filePath = filePath,
                gitProjectId = gitProjectId,
                useAccessToken = true
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == 404) {
                return null
            }
            throw e
        }
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
        state: GitCICommitCheckState
    ) {
        logger.info("CommitCheck with block, gitProjectId:${event.gitProjectId}, mrEvent:$mrEvent, " +
            "block:$block, state:$state, enableMrBlock:${gitProjectConf.enableMrBlock}")
        if (gitProjectConf.enableMrBlock && mrEvent) {
            scmClient.pushCommitCheckWithBlock(
                commitId = event.commitId,
                mergeRequestId = event.mergeRequestId ?: 0L,
                userId = event.userId,
                block = block,
                state = state,
                context = noPipelineBuildEvent,
                gitCIBasicSetting = gitProjectConf,
                jumpRequest = false,
                description = null
            )
        }
    }

    /**
     * push请求时涉及到删除yml文件的操作
     * 所有向远程库的请求最后都会为push，所以针对push删除即可
     * push请求  - 检索当前流水线的存在分支，如果源分支分支在流水线存在分支中唯一，删除流水线
     * 因为源分支已经删除文件，所以后面执行时不会触发构建
     */
    private fun checkAndDeletePipeline(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting
    ) {
        val deleteYamlFiles = (event as GitPushEvent).commits.flatMap {
            if (it.removed != null) {
                it.removed!!.asIterable()
            } else {
                emptyList()
            }
        }.filter { isCiFile(it) }

        if (deleteYamlFiles.isEmpty()) {
            return
        }

        val processClient = client.get(ServicePipelineResource::class)
        deleteYamlFiles.forEach { filePath ->
            val existPipeline = path2PipelineExists[filePath] ?: return@forEach
            val pipelineId = existPipeline.pipelineId
            // 先删除后查询的过程需要加锁
            val redisLock = RedisLock(
                redisOperation,
                "STREAM_DELETE_PIPELINE_$pipelineId",
                60L
            )
            try {
                redisLock.lock()
                gitPipelineBranchService.deleteBranch(pipelineId, gitRequestEvent.branch)
                if (!gitPipelineBranchService.hasBranchExist(pipelineId)) {
                    logger.info("event: ${gitRequestEvent.id} delete file: $filePath with pipeline: $pipelineId ")
                    gitPipelineResourceDao.deleteByPipelineId(dslContext, pipelineId)
                    processClient.delete(gitRequestEvent.userId, gitProjectConf.projectCode!!, pipelineId, channelCode)
                    // 删除相关的构建记录
                    gitCIEventSaveService.deletePipelineBuildHistory(setOf(pipelineId))
                }
            } finally {
                redisLock.unlock()
            }
        }
    }

    private fun handleGetToken(gitRequestEvent: GitRequestEvent, isMrEvent: Boolean = false): String? {
        return triggerExceptionService.handleErrorCode(
            request = gitRequestEvent,
            action = { tokenService.getToken(getProjectId(isMrEvent, gitRequestEvent)) }
        )
    }

    private fun isCiFile(name: String): Boolean {
        if (name == ciFileName) {
            return true
        }
        if (name.startsWith(ciFileDirectoryName) &&
            (name.endsWith(ciFileExtensionYml) || name.endsWith(ciFileExtensionYaml))) {
            return true
        }
        return false
    }

    @Throws(TriggerThirdException::class)
    private fun getCIYamlList(
        gitToken: String,
        gitRequestEvent: GitRequestEvent,
        isMrEvent: Boolean = false
    ): List<String> {
        val ciFileList =
            triggerExceptionService.handleErrorCode(request = gitRequestEvent,
                action = {
                    streamScmService.getFileTreeFromGit(
                        gitToken = gitToken,
                        gitRequestEvent = gitRequestEvent,
                        filePath = ciFileDirectoryName,
                        isMrEvent = isMrEvent
                    )
                }
            )?.filter { it.name.endsWith(ciFileExtensionYml) || it.name.endsWith(ciFileExtensionYaml) }
        return ciFileList?.map { ciFileDirectoryName + File.separator + it.name }?.toList() ?: emptyList()
    }

    @Throws(TriggerThirdException::class)
    private fun isCIYamlExist(
        gitToken: String,
        gitRequestEvent: GitRequestEvent,
        isMrEvent: Boolean = false
    ): Boolean {
        val ciFileList =
            triggerExceptionService.handleErrorCode(request = gitRequestEvent,
                action = { streamScmService.getFileTreeFromGit(gitToken, gitRequestEvent, "", isMrEvent) }
            )?.filter { it.name == ciFileName } ?: emptyList()
        return ciFileList.isNotEmpty()
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
}
