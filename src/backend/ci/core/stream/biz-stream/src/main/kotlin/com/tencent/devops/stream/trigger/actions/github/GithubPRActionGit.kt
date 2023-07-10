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

package com.tencent.devops.stream.trigger.actions.github

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.webhook.enums.code.github.GithubPrEventAction
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.isPrForkEvent
import com.tencent.devops.common.webhook.pojo.code.github.isPrForkNotMergeEvent
import com.tencent.devops.process.yaml.v2.enums.StreamMrEventAction
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.repository.pojo.enums.GithubAccessLevelEnum
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.ChangeYamlList
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCred
import com.tencent.devops.stream.trigger.git.pojo.github.GithubFileInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubMrInfo
import com.tencent.devops.stream.trigger.git.service.GithubApiService
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GithubRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.MrCommentBody
import com.tencent.devops.stream.trigger.pojo.MrYamlInfo
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.GitCheckService
import com.tencent.devops.stream.trigger.service.StreamTriggerTokenService
import com.tencent.devops.stream.util.StreamCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class GithubPRActionGit(
    private val apiService: GithubApiService,
    private val mrConflictCheck: MergeConflictCheck,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService,
    private val streamTriggerTokenService: StreamTriggerTokenService,
    private val basicSettingDao: StreamBasicSettingDao,
    private val dslContext: DSLContext
) : GithubActionGit(apiService, gitCheckService), StreamMrAction {

    companion object {
        private val logger = LoggerFactory.getLogger(GithubPRActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.PULL_REQUEST)

    override fun event() = data.event as GithubPullRequestEvent

    override val mrIId: String
        get() = event().pullRequest.id.toString()

    override fun checkMrForkAction() = event().isPrForkEvent()

    override fun addMrComment(body: MrCommentBody) {
        apiService.addMrComment(
            cred = getGitCred(),
            gitProjectId = getGitProjectIdOrName(data.eventCommon.gitProjectId),
            mrId = event().pullRequest.id.toLong(),
            mrBody = body
        )
    }

    override fun checkMrForkReview(): Boolean {
        if (!event().isPrForkEvent()) return true
        val checkUserAccessLevel = try {
            val accessLevel = this.api.getProjectUserInfo(
                cred = (this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred()) as GithubCred,
                gitProjectId = this.data.eventCommon.gitProjectId,
                userId = this.data.eventCommon.userId
            ).accessLevel

            // >= TRIAGE
            accessLevel >= GithubAccessLevelEnum.TRIAGE.level
        } catch (error: ErrorCodeException) {
            false
        }

        val checkUserInWhiteList = this.data.eventCommon.userId in
            this.data.setting.triggerReviewSetting.whitelist
        val checkProjectInWhiteList = this.data.eventCommon.gitProjectId in
            this.data.setting.triggerReviewSetting.whitelist
        return (
            (checkUserAccessLevel && this.data.setting.triggerReviewSetting.memberNoNeedApproving) ||
                checkUserInWhiteList ||
                checkProjectInWhiteList
            ) && forkMrYamlList().isEmpty()
    }

    override fun getMrId() = event().pullRequest.number.toLong()

    override fun getMrReviewers(): List<String> {
        return event().pullRequest.requestedReviewers.map { it.login }
    }

    override fun forkMrYamlList(): List<ChangeYamlList> {
        return getChangeSet()?.filter { GitActionCommon.checkStreamPipelineAndTemplateFile(it) }?.map {
            ChangeYamlList(
                path = it,
                url = "/files"
            )
        } ?: emptyList()
    }

    override val api: GithubApiService
        get() = apiService

    // 获取Fork库的凭证数据
    private fun getForkGitCred(): GithubCred {
        return streamTriggerTokenService.getGitProjectToken(event().pullRequest.head.repo.id.toString())?.let {
            GithubCred(
                accessToken = it,
                useAccessToken = true,
                userId = null
            )
        } ?: GithubCred(data.setting.enableUser)
    }

    override fun init(): BaseAction? {
        if (data.isSettingInitialized) {
            return initCommonData()
        }
        val setting = basicSettingDao.getSetting(dslContext, event().pullRequest.base.repo.id.toLong())
        if (null == setting || !setting.enableCi) {
            logger.info(
                "git ci is not enabled, but it has repo trigger , " +
                    "git project id: ${event().pullRequest.base.repo.id.toLong()}"
            )
            return null
        }
        data.setting = StreamTriggerSetting(setting)
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val lastCommit = api.getGitCommitInfo(
            cred = if (event.isPrForkEvent()) {
                getForkGitCred()
            } else {
                GithubCred(event.sender.login)
            },
            gitProjectId = getGitProjectIdOrName(event.pullRequest.head.repo.id.toString()),
            sha = event.pullRequest.head.sha,
            retry = ApiRequestRetryInfo(retry = true)
        )
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.pullRequest.base.repo.id.toString(),
            scmType = ScmType.GITHUB,
            sourceGitProjectId = event.pullRequest.head.repo.id.toString(),
            branch = if (event.pullRequest.merged == true) {
                event.pullRequest.base.ref
            } else {
                event.pullRequest.head.ref
            },
            commit = EventCommonDataCommit(
                commitId = event.pullRequest.head.sha,
                commitMsg = lastCommit?.commitMsg,
                commitAuthorName = event.pullRequest.head.user.login,
                commitTimeStamp = lastCommit?.commitDate
            ),
            userId = event.pullRequest.user.login,
            gitProjectName = event.pullRequest.base.repo.fullName,
            eventType = GithubPullRequestEvent.classType
        )
        return this
    }

    override fun tryGetMrInfoFromCache(): GithubMrInfo? {
        // todo
        return null
    }

    override fun needUpdateLastModifyUser(filePath: String) =
        super.needUpdateLastModifyUser(filePath) && !checkMrForkAction()

    override fun isStreamDeleteAction() = false

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        val event = event()
        // 目前不支持Mr信息更新的触发
        if (event.action == GithubPrEventAction.EDITED.value) {
            logger.info("Github web hook is ${event.action} merge request")
            return null
        }

        return GithubRequestEventHandle.createMergeEvent(event, eventStr).copy(
            commitMsg = data.eventCommon.commit.commitMsg,
            commitTimeStamp = data.eventCommon.commit.commitTimeStamp
        )
    }

    override fun skipStream(): Boolean {
        // 目前先把不支持的action全部过滤不触发
        return GithubPrEventAction.get(event()) == GithubPrEventAction.STREAM_NOT_SUPPORT
    }

    override fun checkProjectConfig() {
        if (!data.setting.buildPushedPullRequest) {
            throw StreamTriggerException(this, TriggerReason.BUILD_MERGE_REQUEST_DISABLED)
        }
    }

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean {
        // 已合并的无需检查
        if (event().pullRequest.merged == true) {
            return true
        }
        return true
    }

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {
        if (event().isPrForkNotMergeEvent()) {
            return
        }
        val deleteList = mutableListOf<String>()
        val gitMrChangeInfo = apiService.getMrChangeInfo(
            cred = this.getGitCred(),
            gitProjectId = getGitProjectIdOrName(data.eventCommon.gitProjectId),
            mrId = getMrId().toString(),
            retry = ApiRequestRetryInfo(retry = true)
        )
        gitMrChangeInfo?.files?.forEach { file ->
            if (StreamCommonUtils.isCiFile(file.oldPath) && (file.deletedFile || file.oldPath != file.newPath)) {
                deleteList.add(file.oldPath)
            }
        }
        pipelineDelete.checkAndDeletePipeline(this, path2PipelineExists, deleteList)
    }

    override fun getYamlPathList(): List<YamlPathListEntry> {
        val event = event()
        // 获取目标分支的文件列表
        val targetRef = GitActionCommon.getTriggerBranch(event.pullRequest.base.ref)
        val targetBranchYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = getGitProjectIdOrName(),
            ref = targetRef
        ).toSet()

        // 已经merged的直接返回目标分支的文件列表即可
        if (event.pullRequest.merged == true) {
            return targetBranchYamlPathList.map { (name, blobId) ->
                YamlPathListEntry(name, CheckType.NO_NEED_CHECK, targetRef, blobId)
            }
        }

        // 获取源分支文件列表
        val sourceBranchYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = getGitProjectIdOrName(event.pullRequest.head.repo.id.toString()),
            ref = data.eventCommon.commit.commitId,
            cred = if (event.isPrForkEvent()) {
                getForkGitCred()
            } else {
                null
            }
        ).toSet()

        return checkPrYamlPathList(
            sourceBranchYamlPathList = sourceBranchYamlPathList,
            targetBranchYamlPathList = targetBranchYamlPathList,
            changeSet = getChangeSet()!!,
            sourceRef = event.pullRequest.head.ref,
            targetRef = targetRef
        )
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
    override fun getYamlContent(fileName: String): MrYamlInfo {
        val event = event()
        if (event.pullRequest.merged == true) {
            return MrYamlInfo(
                ref = data.eventCommon.branch,
                content = api.getFileContent(
                    cred = this.getGitCred(),
                    gitProjectId = getGitProjectIdOrName(),
                    fileName = fileName,
                    ref = data.eventCommon.branch,
                    retry = ApiRequestRetryInfo(true)
                ),
                blobId = ""
            )
        }

        val targetFile = getFileInfo(
            cred = getGitCred(),
            gitProjectId = getGitProjectIdOrName(event.pullRequest.base.repo.id.toString()),
            fileName = fileName,
            ref = event.pullRequest.base.ref,
            retry = ApiRequestRetryInfo(true)
        )

        if (!getChangeSet()!!.contains(fileName)) {
            return if (targetFile?.content.isNullOrBlank()) {
                logger.warn(
                    "${data.getGitProjectId()} mr request ${data.context.requestEventId}" +
                        "get file $fileName content from ${event.pullRequest.base.repo.fullName} " +
                        "branch ${event.pullRequest.base.ref} is blank because no file"
                )
                MrYamlInfo(
                    event.pullRequest.base.ref, "", targetFile?.blobId
                )
            } else {
                val c = targetFile!!.getDecodedContentAsString()
                if (c.isBlank()) {
                    logger.warn(
                        "${data.getGitProjectId()} mr request ${data.context.requestEventId}" +
                            "get file $fileName content from ${event.pullRequest.base.repo.fullName} " +
                            "target branch ${event.pullRequest.base.ref} is blank " +
                            "because git content blank"
                    )
                }
                MrYamlInfo(event.pullRequest.base.ref, c, targetFile.blobId)
            }
        }

        val sourceFile = getFileInfo(
            cred = if (event.isPrForkEvent()) {
                getForkGitCred()
            } else {
                getGitCred()
            },
            gitProjectId = getGitProjectIdOrName(event.pullRequest.head.repo.id.toString()),
            fileName = fileName,
            ref = event.pullRequest.head.sha,
            retry = ApiRequestRetryInfo(true)
        )
        val sourceContent = if (sourceFile?.content.isNullOrBlank()) {
            logger.warn(
                "${data.getGitProjectId()} mr request ${data.context.requestEventId}" +
                    "get file $fileName content from ${event.pullRequest.head.repo.fullName} " +
                    "source commit ${event.pullRequest.head.sha} is blank because no file"
            )
            MrYamlInfo(event.pullRequest.head.sha, "", sourceFile?.blobId)
        } else {
            val c = sourceFile!!.getDecodedContentAsString()
            if (c.isBlank()) {
                logger.warn(
                    "${data.getGitProjectId()} mr request ${data.context.requestEventId}" +
                        "get file $fileName content from ${event.pullRequest.head.repo.fullName} " +
                        "source commit ${event.pullRequest.head.sha} is blank " +
                        "because git content blank"
                )
            }
            MrYamlInfo(event.pullRequest.head.sha, c, sourceFile.blobId)
        }

        if (targetFile?.blobId.isNullOrBlank()) {
            return sourceContent
        }

        if (targetFile?.blobId == sourceFile?.blobId) {
            return sourceContent
        }

        val mergeRequest = tryGetMrInfoFromCache() ?: apiService.getMrInfo(
            cred = getGitCred(),
            gitProjectId = getGitProjectIdOrName(event.pullRequest.base.repo.id.toString()),
            mrId = this.getMrId().toString(),
            retry = ApiRequestRetryInfo(true)
        )!!
        val baseTargetFile = getFileInfo(
            cred = getGitCred(),
            gitProjectId = getGitProjectIdOrName(event.pullRequest.base.repo.id.toString()),
            fileName = fileName,
            ref = mergeRequest.baseCommit,
            retry = ApiRequestRetryInfo(true)
        )
        if (targetFile?.blobId == baseTargetFile?.blobId) {
            return sourceContent
        }
        throw StreamTriggerException(
            this,
            TriggerReason.CI_YAML_NEED_MERGE_OR_REBASE,
            reasonParams = listOf(fileName),
            commitCheck = CommitCheck(
                block = true,
                state = StreamCommitCheckState.FAILURE
            )
        )
    }

    override fun getChangeSet(): Set<String>? {
        // 使用null和empty的区别来判断是否调用过获取函数
        if (this.data.context.changeSet != null) {
            return this.data.context.changeSet
        }

        // 获取mr请求的变更文件列表，用来给后面判断
        val changeSet = mutableSetOf<String>()
        apiService.getMrChangeInfo(
            cred = (this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred()) as GithubCred,
            // 获取mr信息的project Id和事件强关联，不一定是流水线所处库
            gitProjectId = getGitProjectIdOrName(data.eventCommon.gitProjectId),
            mrId = getMrId().toString(),
            retry = ApiRequestRetryInfo(true)
        )?.files?.forEach {
            if (it.deletedFile) {
                changeSet.add(it.oldPath)
            } else if (it.renameFile) {
                changeSet.add(it.oldPath)
                changeSet.add(it.newPath)
            } else {
                changeSet.add(it.newPath)
            }
        }

        this.data.context.changeSet = changeSet

        return this.data.context.changeSet
    }

    private fun getFileInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): GithubFileInfo? {
        return try {
            apiService.getFileInfo(
                cred = cred,
                gitProjectId = gitProjectId,
                fileName = fileName,
                ref = ref,
                retry = retry
            )
        } catch (e: ErrorCodeException) {
            if (e.statusCode == 404) {
                return null
            }
            throw e
        }
    }

    @Suppress("ComplexMethod")
    fun checkPrYamlPathList(
        sourceBranchYamlPathList: Set<Pair<String, String?>>,
        targetBranchYamlPathList: Set<Pair<String, String?>>,
        changeSet: Set<String>,
        sourceRef: String,
        targetRef: String
    ): List<YamlPathListEntry> {
        val sourceList = sourceBranchYamlPathList.map { it.first }
        val targetList = targetBranchYamlPathList.map { it.first }
        val result = mutableListOf<YamlPathListEntry>()

        sourceBranchYamlPathList.forEach { (source, blobId) ->
            when {
                // 源分支有，目标分支没有，变更列表有，以源分支为主，不需要校验版本
                source !in targetList && source in changeSet -> {
                    result.add(YamlPathListEntry(source, CheckType.NO_NEED_CHECK, sourceRef, blobId))
                }
                // 源分支有，目标分支没有，变更列表没有，不触发且提示错误
                source !in targetList && source !in changeSet -> {
                    result.add(YamlPathListEntry(source, CheckType.NO_TRIGGER, sourceRef, blobId))
                }
                // 源分支有，目标分支有，变更列表有，需要校验版本
                source in targetList && source in changeSet -> {
                    result.add(YamlPathListEntry(source, CheckType.NEED_CHECK, sourceRef, blobId))
                }
                // 源分支有，目标分支有，变更列表无，以目标分支为主，不需要校验版本
                source in targetList && source !in changeSet -> {
                    result.add(YamlPathListEntry(source, CheckType.NO_NEED_CHECK, targetRef, blobId))
                }
            }
        }

        targetBranchYamlPathList.forEach { (target, blobId) ->
            if (target in result.map { it.yamlPath }.toSet()) {
                return@forEach
            }
            when {
                // 源分支没有，目标分支有，变更列表有，说明是删除，无需触发
                target !in sourceList && target in changeSet -> {
                    return@forEach
                }
                // 源分支没有，目标分支有，变更列表没有，说明是目标分支新增的，加入文件列表
                target !in sourceList && target !in changeSet -> {
                    result.add(YamlPathListEntry(target, CheckType.NO_NEED_CHECK, targetRef, blobId))
                }
            }
        }
        return result
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val event = event()
        val mrAction = event.getActionValue() ?: false
        val isMatch = TriggerMatcher.isMrMatch(
            triggerOn = triggerOn,
            sourceBranch = GitActionCommon.getTriggerBranch(event.pullRequest.head.ref),
            targetBranch = GitActionCommon.getTriggerBranch(event.pullRequest.base.ref),
            changeSet = getChangeSet(),
            userId = data.eventCommon.userId,
            mrAction = mrAction
        )
        return TriggerResult(
            trigger = isMatch,
            triggerOn = triggerOn,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return GitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
    }

    override fun needSaveOrUpdateBranch() = !event().isPrForkEvent()

    override fun sendUnlockWebhook() = Unit
}

private fun GithubPullRequestEvent.getActionValue(): String? {
    return when (GithubPrEventAction.get(this)) {
        GithubPrEventAction.OPEN -> StreamMrEventAction.OPEN.value
        GithubPrEventAction.CLOSE -> StreamMrEventAction.CLOSE.value
        GithubPrEventAction.REOPEN -> StreamMrEventAction.REOPEN.value
        GithubPrEventAction.PUSH_UPDATE -> StreamMrEventAction.PUSH_UPDATE.value
        GithubPrEventAction.MERGE -> StreamMrEventAction.MERGE.value
        else -> null
    }
}
