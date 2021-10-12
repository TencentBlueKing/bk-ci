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
import com.tencent.devops.stream.common.exception.CommitCheck
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
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.stream.v2.service.ScmService
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitMergeActionKind
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitObjectKind
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitPushActionKind
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitPushOperationKind
import com.tencent.devops.stream.pojo.isFork
import com.tencent.devops.stream.trigger.parsers.MergeConflict
import com.tencent.devops.stream.trigger.parsers.YamlVersion
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.triggerParameter.TriggerParameter
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

@Suppress("ComplexCondition")
@Service
class GitCITriggerService @Autowired constructor(
    private val scmClient: ScmClient,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCIBasicSettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val rabbitTemplate: RabbitTemplate,
    private val yamlTriggerFactory: YamlTriggerFactory,
    private val scmService: ScmService,
    private val triggerParameter: TriggerParameter,
    private val mergeConflict: MergeConflict,
    private val yamlVersion: YamlVersion,
    private val pipelineDelete: PipelineDelete,
    private val triggerExceptionService: TriggerExceptionService
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

        CheckStreamSetting.checkGitProjectConf(gitRequestEvent, event, gitProjectConf)

        val path2PipelineExists = gitPipelineResourceDao.getAllByGitProjectId(dslContext, gitProjectConf.gitProjectId)
            .associate {
                it.filePath to GitProjectPipeline(
                    gitProjectId = it.gitProjectId,
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildInfo = null,
                    latestBuildBranch = null
                )
            }

        // 校验mr请求是否产生冲突，已合并的无需检查
        if (event is GitMergeRequestEvent &&
            event.object_attributes.action != TGitMergeActionKind.MERGE.value &&
            !mergeConflict.checkMrConflict(
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
        val isFork = !isMerged && gitRequestEvent.isFork()
        var forkGitToken: GitToken? = null
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

        // 判断本次mr/push提交是否需要删除流水线, fork不用
        if (event is GitPushEvent || event is GitMergeRequestEvent && !isFork) {
            pipelineDelete.checkAndDeletePipeline(
                gitRequestEvent = gitRequestEvent,
                event = event,
                path2PipelineExists = path2PipelineExists,
                gitProjectConf = gitProjectConf,
                gitToken = gitToken.accessToken
            )
        }
        // TODO:对于这种只是为了做一些非构建的特殊操作，后续可以抽出一层在构建逻辑前单独维护
        if (isDeleteBranch(gitRequestEvent)) {
            return true
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
                    scmService.getMergeRequestChangeInfo(
                        userId = null,
                        token = gitToken.accessToken,
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
                    latestBuildInfo = null,
                    latestBuildBranch = null
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
                            gitProjectConf = gitProjectConf
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
        forkGitToken: GitToken?,
        gitToken: GitToken,
        changeSet: Set<String>,
        displayName: String,
        mrEvent: Boolean,
        isMerged: Boolean,
        gitProjectConf: GitCIBasicSetting
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
            // todo: 将超级token根据项目ID塞到Map里，每次取一下，没有了就重新拿
            val (result, orgYaml) =
                yamlVersion.checkYmlVersion(
                    mrEvent = event as GitMergeRequestEvent,
                    sourceGitToken = forkGitToken?.accessToken,
                    targetGitToken = gitToken.accessToken,
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
                    reasonParams = listOf(filePath),
                    commitCheck = CommitCheck(
                        block = mrEvent,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            }
            orgYaml
        } else {
            scmService.getYamlFromGit(
                token = forkGitToken?.accessToken ?: gitToken.accessToken,
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
                    gitToken = gitToken,
                    forkGitToken = forkGitToken,
                    gitRequestEvent = gitRequestEvent,
                    gitProjectPipeline = buildPipeline,
                    event = event,
                    originYaml = originYaml,
                    filePath = filePath,
                    gitCIBasicSetting = gitProjectConf,
                    changeSet = changeSet
                )
            )
        } else {
            triggerInterface.triggerBuild(
                gitToken = gitToken,
                forkGitToken = forkGitToken,
                gitRequestEvent = gitRequestEvent,
                gitProjectPipeline = buildPipeline,
                event = event,
                originYaml = originYaml,
                filePath = filePath,
                changeSet = null
            )
        }
    }

    private fun dispatchStreamTrigger(event: StreamTriggerEvent) {
        StreamTriggerDispatch.dispatch(rabbitTemplate, event)
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

    private fun handleGetToken(gitRequestEvent: GitRequestEvent, isMrEvent: Boolean = false): GitToken? {
        return triggerExceptionService.handleErrorCode(
            request = gitRequestEvent,
            action = { scmService.getToken(getProjectId(isMrEvent, gitRequestEvent).toString()) }
        )
    }

    @Throws(TriggerThirdException::class)
    private fun getCIYamlList(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        isMrEvent: Boolean = false
    ): List<String> {
        val ciFileList =
            triggerExceptionService.handleErrorCode(request = gitRequestEvent,
                action = { scmService.getFileTreeFromGit(gitToken, gitRequestEvent, ciFileDirectoryName, isMrEvent) }
            )?.filter { it.name.endsWith(ciFileExtensionYml) || it.name.endsWith(ciFileExtensionYaml) }
        return ciFileList?.map { ciFileDirectoryName + File.separator + it.name }?.toList() ?: emptyList()
    }

    @Throws(TriggerThirdException::class)
    private fun isCIYamlExist(
        gitToken: GitToken,
        gitRequestEvent: GitRequestEvent,
        isMrEvent: Boolean = false
    ): Boolean {
        val ciFileList =
            triggerExceptionService.handleErrorCode(request = gitRequestEvent,
                action = { scmService.getFileTreeFromGit(gitToken, gitRequestEvent, "", isMrEvent) }
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

    // 判断是否是删除分支的event这个Event不做构建只做删除逻辑
    private fun isDeleteBranch(requestEvent: GitRequestEvent): Boolean {
        return requestEvent.objectKind == TGitObjectKind.PUSH.value &&
                requestEvent.operationKind == TGitPushOperationKind.DELETE.value &&
                requestEvent.extensionAction == TGitPushActionKind.DELETE_BRANCH.value
    }
}
