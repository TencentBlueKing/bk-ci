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
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.repository.pojo.oauth.GitToken
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerException.Companion.triggerError
import com.tencent.devops.stream.common.exception.TriggerThirdException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.config.StreamStorageBean
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.mq.streamTrigger.StreamTriggerDispatch
import com.tencent.devops.stream.mq.streamTrigger.StreamTriggerEvent
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.isFork
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.trigger.parsers.CheckStreamSetting
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.PreTrigger
import com.tencent.devops.stream.trigger.parsers.YamlVersion
import com.tencent.devops.stream.trigger.parsers.triggerParameter.TriggerParameter
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.pojo.StreamTriggerContext
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import com.tencent.devops.stream.v2.service.DeleteEventService
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import com.tencent.devops.stream.v2.service.StreamScmService
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
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val streamStorageBean: StreamStorageBean,
    private val gitCISettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val rabbitTemplate: RabbitTemplate,
    private val yamlTriggerFactory: YamlTriggerFactory,
    private val streamScmService: StreamScmService,
    private val preTrigger: PreTrigger,
    private val mergeConflictCheck: MergeConflictCheck,
    private val yamlVersion: YamlVersion,
    private val pipelineDelete: PipelineDelete,
    private val triggerExceptionService: TriggerExceptionService,
    private val tokenService: StreamGitTokenService,
    private val deleteEventService: DeleteEventService,
    private val triggerParameter: TriggerParameter,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val streamTriggerCache: StreamTriggerCache
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerService::class.java)
        private const val ciFileExtensionYml = ".yml"
        private const val ciFileExtensionYaml = ".yaml"
        private const val ciFileName = ".ci.yml"
        private const val ciFileDirectoryName = ".ci"
    }

    fun externalCodeGitBuild(event: String): Boolean? {
        val start = LocalDateTime.now().timestampmilli()
        logger.info("Trigger code git build($event)")
        val eventObject = try {
            objectMapper.readValue<GitEvent>(event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
            return false
        }

        val gitRequestEvent = triggerParameter.getGitRequestEvent(eventObject, event) ?: return true

        // 做一些在接收到请求后做的预处理
        if (gitRequestEvent.objectKind == TGitObjectKind.PUSH.value) {
            preTrigger.enableAtomCi(gitRequestEvent, (eventObject as GitPushEvent).repository)
        }

        val gitCIBasicSetting = gitCISettingDao.getSetting(dslContext, gitRequestEvent.gitProjectId)
        // 没开启的就不存
        if (null == gitCIBasicSetting || !gitCIBasicSetting.enableCi) {
            logger.info("git ci is not enabled, git project id: ${gitRequestEvent.gitProjectId}")
            return null
        }

        // 创建过项目的才保存记录继续后面的逻辑
        val id = gitRequestEventDao.saveGitRequest(dslContext, gitRequestEvent)
        gitRequestEvent.id = id

        if (eventObject is GitPushEvent && preTrigger.skipStream(eventObject)) {
            logger.info("project: ${gitRequestEvent.gitProjectId} commit: ${gitRequestEvent.commitId} skip ci")
            return true
        }

        streamStorageBean.saveRequestTime(LocalDateTime.now().timestampmilli() - start)

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

        streamStorageBean.pipelineAndConflictTime(LocalDateTime.now().timestampmilli() - start)

        return matchAndTriggerPipeline(gitRequestEvent, event, path2PipelineExists, gitProjectConf)
    }

    @Suppress("ALL")
    fun matchAndTriggerPipeline(
        gitRequestEvent: GitRequestEvent,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting
    ): Boolean {
        val start = LocalDateTime.now().timestampmilli()

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
        var forkGitToken: String? = null
        var forkGitProjectId: Long? = null
        if (isFork) {
            forkGitToken = handleGetToken(gitRequestEvent, true)!!
            forkGitProjectId = getProjectId(mrEvent, gitRequestEvent)
            logger.info(
                "get fork token for gitProject[$forkGitProjectId] form scm, token: $forkGitToken"
            )
        }

        // 判断本次mr/push提交是否需要删除流水线, fork不用
        if (event is GitPushEvent || event is GitMergeRequestEvent && !isFork) {
            pipelineDelete.checkAndDeletePipeline(
                gitRequestEvent = gitRequestEvent,
                event = event,
                path2PipelineExists = path2PipelineExists,
                gitProjectConf = gitProjectConf,
                gitToken = gitToken
            )
        }

        val isDeleteEvent = event.isDeleteEvent()

        val gitProjectInfoCache = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitRequestEventId = gitRequestEvent.id!!,
            gitProjectId = gitRequestEvent.gitProjectId.toString(),
            token = gitToken,
            useAccessToken = true,
            getProjectInfo = streamScmService::getProjectInfoRetry
        )

        val yamlPathList = if (isDeleteEvent) {
            getYamlPathList(
                isFork = false,
                forkGitToken = null,
                gitRequestEvent = gitRequestEvent.copy(branch = gitProjectInfoCache.defaultBranch ?: ""),
                mrEvent = false,
                gitToken = gitToken
            )
        } else {
            getYamlPathList(isFork, forkGitToken, gitRequestEvent, mrEvent, gitToken)
        }

        logger.info(
            "matchAndTriggerPipeline in gitProjectId:${gitProjectConf.gitProjectId}, yamlPathList: " +
                "$yamlPathList, path2PipelineExists: $path2PipelineExists, " +
                "commitTime:${gitRequestEvent.commitTimeStamp}, " +
                "hookStartTime:${DateTimeUtil.toDateTime(hookStartTime)}, " +
                "yamlCheckedTime:${DateTimeUtil.toDateTime(LocalDateTime.now())}"
        )

        // 如果没有Yaml文件则直接不触发
        if (yamlPathList.isEmpty()) {
            logger.warn("event: ${gitRequestEvent.id} cannot found ci yaml from git")
            triggerError(
                request = gitRequestEvent,
                reason = TriggerReason.CI_YAML_NOT_FOUND
            )
        }

        // mr提交锁定,这时还没有流水线，所以提交的是无流水线锁
        // story_871153869 暂时下掉mr锁，看效果，后续需要再加
//        blockCommitCheck(
//            mrEvent = mrEvent,
//            event = gitRequestEvent,
//            gitProjectConf = gitProjectConf,
//            block = true,
//            state = GitCICommitCheckState.PENDING
//        )

        // 获取mr请求的变更文件列表，用来给后面判断
        val changeSet = if (mrEvent) {
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

        streamStorageBean.yamlListCheckTime(LocalDateTime.now().timestampmilli() - start)

        yamlPathList.forEach { filePath ->
            // 如果该流水线已保存过，则继续使用
            // 对于来自fork库的mr新建的流水线，当前库不维护其状态
            val buildPipeline = path2PipelineExists[filePath] ?: GitProjectPipeline(
                gitProjectId = gitProjectConf.gitProjectId,
                displayName = filePath,
                pipelineId = "", // 留空用于是否创建判断
                filePath = filePath,
                enabled = true,
                creator = gitRequestEvent.userId,
                latestBuildInfo = null,
                latestBuildBranch = null
            )
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
                            displayName = filePath,
                            mrEvent = mrEvent,
                            isMerged = isMerged,
                            gitProjectConf = gitProjectConf,
                            forkGitProjectId = forkGitProjectId,
                            defaultBranch = gitProjectInfoCache.defaultBranch
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
        // story_871153869 暂时下掉mr锁，看效果，后续需要再加
//        blockCommitCheck(
//            mrEvent = mrEvent,
//            event = gitRequestEvent,
//            gitProjectConf = gitProjectConf,
//            block = false,
//            state = GitCICommitCheckState.SUCCESS
//        )
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
        forkGitProjectId: Long?,
        defaultBranch: String?
    ) {
        val start = LocalDateTime.now().timestampmilli()

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
                yamlVersion.checkYmlVersion(
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
                    reasonParams = listOf(filePath),
                    commitCheck = CommitCheck(
                        block = mrEvent,
                        state = GitCICommitCheckState.FAILURE
                    )
                )
            }
            orgYaml
        } else {
            if (event.isDeleteEvent()) {
                streamScmService.getYamlFromGit(
                    token = forkGitToken ?: gitToken,
                    ref = defaultBranch ?: "",
                    fileName = filePath,
                    gitProjectId = getProjectId(mrEvent, gitRequestEvent).toString(),
                    useAccessToken = true
                )
            } else {
                streamScmService.getYamlFromGit(
                    token = forkGitToken ?: gitToken,
                    ref = gitRequestEvent.branch,
                    fileName = filePath,
                    gitProjectId = getProjectId(mrEvent, gitRequestEvent).toString(),
                    useAccessToken = true
                )
            }
        }

        // 组装上下文参数
        val context = StreamTriggerContext(
            gitEvent = event,
            requestEvent = gitRequestEvent,
            streamSetting = gitProjectConf,
            pipeline = buildPipeline,
            originYaml = originYaml,
            mrChangeSet = changeSet
        )

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

        yamlSchemaCheck.check(context = context, templateType = null, isCiFile = true)

        // 为已存在的流水线设置名称
        buildPipeline.displayName = displayName

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
                    changeSet = changeSet,
                    forkGitProjectId = forkGitProjectId,
                    // TODO 为了保证消息生产消费兼容，下次发布再去掉event的token字段
                    gitToken = GitToken(accessToken = gitToken),
                    forkGitToken = GitToken(accessToken = forkGitToken ?: "")
                )
            )
        } else {
            triggerInterface.triggerBuild(
                context
            )
        }
        streamStorageBean.triggerCheckTime(LocalDateTime.now().timestampmilli() - start)
    }

    private fun getYamlPathList(
        isFork: Boolean,
        forkGitToken: String?,
        gitRequestEvent: GitRequestEvent,
        mrEvent: Boolean,
        gitToken: String
    ): MutableList<String> {
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
        return yamlPathList
    }

    private fun dispatchStreamTrigger(event: StreamTriggerEvent) {
        StreamTriggerDispatch.dispatch(rabbitTemplate, event)
    }

    // mr锁定提交
    // story_871153869 暂时下掉mr锁，看效果，后续需要再加
//    private fun blockCommitCheck(
//        mrEvent: Boolean,
//        event: GitRequestEvent,
//        gitProjectConf: GitCIBasicSetting,
//        block: Boolean,
//        state: GitCICommitCheckState
//    ) {
//        logger.info(
//            "CommitCheck with block, gitProjectId:${event.gitProjectId}, mrEvent:$mrEvent, " +
//                    "block:$block, state:$state, enableMrBlock:${gitProjectConf.enableMrBlock}"
//        )
//        if (gitProjectConf.enableCommitCheck && gitProjectConf.enableMrBlock && mrEvent) {
//            scmClient.pushCommitCheckWithBlock(
//                commitId = event.commitId,
//                mergeRequestId = event.mergeRequestId ?: 0L,
//                userId = event.userId,
//                block = block,
//                state = state,
//                context = noPipelineBuildEvent,
//                gitCIBasicSetting = gitProjectConf,
//                jumpNotification = false,
//                description = null
//            )
//        }
//    }

    private fun handleGetToken(gitRequestEvent: GitRequestEvent, isMrEvent: Boolean = false): String? {
        return triggerExceptionService.handleErrorCode(
            request = gitRequestEvent,
            action = { tokenService.getToken(getProjectId(isMrEvent, gitRequestEvent)) }
        )
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
