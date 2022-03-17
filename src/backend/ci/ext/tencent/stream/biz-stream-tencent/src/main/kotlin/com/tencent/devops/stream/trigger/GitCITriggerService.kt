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
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.StreamTriggerContext
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
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
    private val triggerParameter: TriggerParameter,
    private val yamlSchemaCheck: YamlSchemaCheck,
    private val streamTriggerCache: StreamTriggerCache,
    private val gitCIEventService: GitCIEventService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCITriggerService::class.java)
        private const val ciFileExtensionYml = ".yml"
        private const val ciFileExtensionYaml = ".yaml"
        private const val ciFileName = ".ci.yml"
        private const val ciFileDirectoryName = ".ci"
    }

    fun externalCodeGitBuild(eventType: String?, event: String): Boolean? {
        val start = LocalDateTime.now().timestampmilli()
        logger.info("Trigger code git build($event, $eventType)")
        val eventObject = try {
            objectMapper.readValue<GitEvent>(event)
        } catch (ignore: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${ignore.message}")
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
            gitCIEventService.saveTriggerNotBuildEvent(
                userId = gitRequestEvent.userId,
                eventId = gitRequestEvent.id!!,
                reason = TriggerReason.USER_SKIPED.name,
                reasonDetail = TriggerReason.USER_SKIPED.detail,
                gitProjectId = gitRequestEvent.gitProjectId,
                branch = gitRequestEvent.branch
            )
            return true
        }

        streamStorageBean.saveRequestTime(LocalDateTime.now().timestampmilli() - start)

        return triggerExceptionService.handle(gitRequestEvent, eventObject, gitCIBasicSetting) {
            triggerExceptionService.handleErrorCode(request = gitRequestEvent) {
                checkRequest(gitRequestEvent, eventObject, gitCIBasicSetting)
            }
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

        val (yamlPathList, changeSet) = if (isDeleteEvent) {
            Pair(
                getYamlPathList(
                    gitProjectId = gitRequestEvent.gitProjectId,
                    gitToken = gitToken,
                    ref = gitProjectInfoCache.defaultBranch
                ).map { YamlPathListEntry(it, CheckType.NO_NEED_CHECK) }, emptySet()
            )
        } else if (event is GitMergeRequestEvent) {
            getMrYamlPathList(
                isFork = isFork,
                forkGitToken = forkGitToken,
                gitRequestEvent = gitRequestEvent,
                gitToken = gitToken,
                targetBranch = event.object_attributes.target_branch,
                mrId = event.object_attributes.id,
                merged = isMerged
            )
        } else {
            Pair(
                getYamlPathList(
                    gitProjectId = gitRequestEvent.gitProjectId,
                    gitToken = gitToken,
                    ref = gitRequestEvent.branch
                ).map { YamlPathListEntry(it, CheckType.NO_NEED_CHECK) }, emptySet()
            )
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

        streamStorageBean.yamlListCheckTime(LocalDateTime.now().timestampmilli() - start)

        yamlPathList.forEach { (filePath, checkType) ->
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
                // 目前只针对mr情况下源分支有目标分支没有且变更列表没有
                if (checkType == CheckType.NO_TRIGGER) {
                    triggerError(
                        request = gitRequestEvent,
                        reason = TriggerReason.MR_BRANCH_FILE_ERROR,
                        reasonParams = listOf(filePath)
                    )
                }

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

    private fun getMrYamlPathList(
        isFork: Boolean,
        forkGitToken: String?,
        targetBranch: String,
        gitRequestEvent: GitRequestEvent,
        gitToken: String,
        mrId: Long,
        merged: Boolean
    ): Pair<List<YamlPathListEntry>, Set<String>> {
        // 获取目标分支的文件列表
        val targetBranchYamlPathList = getYamlPathList(
            gitProjectId = gitRequestEvent.gitProjectId,
            gitToken = gitToken,
            ref = streamScmService.getTriggerBranch(targetBranch)
        ).toSet()

        // 获取mr请求的变更文件列表，用来给后面判断
        val changeSet = streamScmService.getMergeRequestChangeInfo(
            userId = null,
            token = gitToken,
            gitProjectId = gitRequestEvent.gitProjectId,
            mrId = mrId
        )?.files?.map {
            if (it.deletedFile) {
                it.oldPath
            } else {
                it.newPath
            }
        }?.toSet() ?: emptySet()

        // 已经merged的直接返回目标分支的文件列表即可
        if (merged) {
            return Pair(targetBranchYamlPathList.map { YamlPathListEntry(it, CheckType.NO_NEED_CHECK) }, changeSet)
        }

        // 获取源分支文件列表
        val sourceBranchYamlPathList = getYamlPathList(
            gitProjectId = gitRequestEvent.sourceGitProjectId!!,
            gitToken = if (isFork) {
                forkGitToken!!
            } else {
                gitToken
            },
            ref = gitRequestEvent.commitId
        ).toSet()

        val comparedMap = checkMrYamlPathList(sourceBranchYamlPathList, targetBranchYamlPathList, changeSet)
        return Pair(comparedMap.map { YamlPathListEntry(it.key, it.value) }, changeSet)
    }

    @Suppress("ComplexMethod")
    fun checkMrYamlPathList(
        sourceBranchYamlPathList: Set<String>,
        targetBranchYamlPathList: Set<String>,
        changeSet: Set<String>
    ): MutableMap<String, CheckType> {
        val comparedMap = mutableMapOf<String, CheckType>()
        sourceBranchYamlPathList.forEach { source ->
            when {
                // 源分支有，目标分支没有，变更列表有，以源分支为主，不需要校验版本
                source !in targetBranchYamlPathList && source in changeSet -> {
                    comparedMap[source] = CheckType.NO_NEED_CHECK
                }
                // 源分支有，目标分支没有，变更列表没有，不触发且提示错误
                source !in targetBranchYamlPathList && source !in changeSet -> {
                    comparedMap[source] = CheckType.NO_TRIGGER
                }
                // 源分支有，目标分支有，变更列表有，需要校验版本
                source in targetBranchYamlPathList && source in changeSet -> {
                    comparedMap[source] = CheckType.NEED_CHECK
                }
                // 源分支有，目标分支有，变更列表无，以目标分支为主，不需要校验版本
                source in targetBranchYamlPathList && source !in changeSet -> {
                    comparedMap[source] = CheckType.NO_NEED_CHECK
                }
            }
        }
        targetBranchYamlPathList.forEach { target ->
            if (target in comparedMap.keys) {
                return@forEach
            }
            when {
                // 源分支没有，目标分支有，变更列表有，说明是删除，无需触发
                target !in sourceBranchYamlPathList && target in changeSet -> {
                    return@forEach
                }
                // 源分支没有，目标分支有，变更列表没有，说明是目标分支新增的，加入文件列表
                target !in sourceBranchYamlPathList && target !in changeSet -> {
                    comparedMap[target] = CheckType.NO_NEED_CHECK
                }
            }
        }
        return comparedMap
    }

    private fun getYamlPathList(
        gitProjectId: Long,
        gitToken: String,
        ref: String?
    ): MutableList<String> {
        // 获取指定目录下所有yml文件
        val yamlPathList = getCIYamlList(gitProjectId, gitToken, ref).toMutableList()

        // 兼容旧的根目录yml文件
        val isCIYamlExist = isCIYamlExist(gitProjectId, gitToken, ref)

        if (isCIYamlExist) {
            yamlPathList.add(ciFileName)
        }
        return yamlPathList
    }

    private fun dispatchStreamTrigger(event: StreamTriggerEvent) {
        StreamTriggerDispatch.dispatch(rabbitTemplate, event)
    }

    private fun handleGetToken(gitRequestEvent: GitRequestEvent, isMrEvent: Boolean = false): String? {
        return triggerExceptionService.handleErrorCode(
            request = gitRequestEvent,
            action = { tokenService.getToken(getProjectId(isMrEvent, gitRequestEvent)) }
        )
    }

    @Throws(TriggerThirdException::class)
    private fun getCIYamlList(
        gitProjectId: Long,
        gitToken: String,
        ref: String?
    ): List<String> {
        val ciFileList = streamScmService.getFileTreeFromGit(
            gitProjectId = gitProjectId,
            token = gitToken,
            filePath = ciFileDirectoryName,
            ref = ref?.let { streamScmService.getTriggerBranch(it) }
        ).filter { it.name.endsWith(ciFileExtensionYml) || it.name.endsWith(ciFileExtensionYaml) }
        return ciFileList.map { ciFileDirectoryName + File.separator + it.name }.toList()
    }

    @Throws(TriggerThirdException::class)
    private fun isCIYamlExist(
        gitProjectId: Long,
        gitToken: String,
        ref: String?
    ): Boolean {
        val ciFileList = streamScmService.getFileTreeFromGit(
            gitProjectId = gitProjectId,
            token = gitToken,
            filePath = "",
            ref = ref?.let { streamScmService.getTriggerBranch(it) }
        ).filter { it.name == ciFileName }
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
