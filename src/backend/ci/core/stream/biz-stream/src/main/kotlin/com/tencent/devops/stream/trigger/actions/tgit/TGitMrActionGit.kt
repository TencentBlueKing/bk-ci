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

package com.tencent.devops.stream.trigger.actions.tgit

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeExtensionActionKind
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.common.webhook.pojo.code.git.isMrForkEvent
import com.tencent.devops.common.webhook.pojo.code.git.isMrForkNotMergeEvent
import com.tencent.devops.common.webhook.pojo.code.git.isMrMergeEvent
import com.tencent.devops.process.yaml.v2.enums.StreamMrEventAction
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.WebhookCommit
import com.tencent.devops.scm.utils.code.git.GitUtils
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
import com.tencent.devops.stream.trigger.actions.data.context.StreamMrInfo
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrInfo
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
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
import java.util.Base64
import java.util.Date

class TGitMrActionGit(
    private val dslContext: DSLContext,
    private val streamSettingDao: StreamBasicSettingDao,
    private val apiService: TGitApiService,
    private val mrConflictCheck: MergeConflictCheck,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService,
    private val streamTriggerTokenService: StreamTriggerTokenService
) : TGitActionGit(apiService, gitCheckService), StreamMrAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitMrActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.MERGE_REQUEST)

    override fun event() = data.event as GitMergeRequestEvent

    override val mrIId: String
        get() = event().object_attributes.iid.toString()

    override fun checkMrForkAction() = event().isMrForkEvent()

    override fun addMrComment(body: MrCommentBody) {
        apiService.addMrComment(
            cred = getGitCred(),
            gitProjectId = data.eventCommon.gitProjectId,
            mrId = event().object_attributes.id,
            mrBody = body
        )
    }

    override fun checkMrForkReview(): Boolean {
        if (!event().isMrForkEvent()) return true
        val checkUserAccessLevel = try {
            val accessLevel = this.api.getProjectMember(
                cred = (this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred()) as TGitCred,
                gitProjectId = this.data.eventCommon.gitProjectId,
                search = this.data.eventCommon.userId
            ).find { it.userId == this.data.eventCommon.userId }?.accessLevel

            accessLevel != null && accessLevel >= GitAccessLevelEnum.DEVELOPER.level
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

    override fun getMrId() = event().object_attributes.id

    override fun getMrReviewers(): List<String> {
        return try {
            val mrReviewInfo = data.context.gitMrReviewInfo ?: api.getMrReview(
                cred = getGitCred(),
                gitProjectId = event().object_attributes.target_project_id.toString(),
                mrId = event().object_attributes.id.toString(),
                retry = ApiRequestRetryInfo(true)
            )
            mrReviewInfo?.reviewers?.map { it.username } ?: emptyList()
        } catch (e: Throwable) {
            logger.error("tGit get mr reviewers error: ${e.message}", e)
            emptyList()
        }
    }

    override fun forkMrYamlList(): List<ChangeYamlList> {
        return getChangeSet()?.filter { GitActionCommon.checkStreamPipelineAndTemplateFile(it) }?.map {
            ChangeYamlList(
                path = it,
                url = "/diff/$it"
            )
        } ?: emptyList()
    }

    override val api: TGitApiService
        get() = apiService

    // 获取Fork库的凭证数据
    private fun getForkGitCred(): TGitCred {
        return streamTriggerTokenService.getGitProjectToken(data.eventCommon.sourceGitProjectId)?.let {
            TGitCred(
                accessToken = it,
                useAccessToken = true,
                userId = null
            )
        } ?: TGitCred(data.setting.enableUser)
    }

    override fun init(): BaseAction {
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.object_attributes.target_project_id.toString(),
            scmType = ScmType.CODE_GIT,
            sourceGitProjectId = event.object_attributes.source_project_id.toString(),
            branch = if (event.object_attributes.action == TGitMergeActionKind.MERGE.value) {
                event.object_attributes.target_branch
            } else {
                event.object_attributes.source_branch
            },
            commit = EventCommonDataCommit(
                commitId = event.object_attributes.last_commit.id,
                commitMsg = event.object_attributes.last_commit.message,
                commitAuthorName = event.object_attributes.last_commit.author.name,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(event.object_attributes.last_commit.timestamp)
            ),
            userId = event.user.username,
            gitProjectName = GitUtils.getProjectName(event.object_attributes.target.http_url)
        )
        return this
    }

    override fun initCacheData() {
        val event = event()
        // 初始化setting
        if (!data.isSettingInitialized) {
            val gitCIBasicSetting = streamSettingDao.getSetting(dslContext, event.object_attributes.target_project_id)
            if (null != gitCIBasicSetting) {
                data.setting = StreamTriggerSetting(gitCIBasicSetting)
            }
        }
        if (data.isSettingInitialized) {
            try {
                data.context.gitMrInfo = apiService.getMrInfo(
                    cred = getGitCred(),
                    gitProjectId = data.eventCommon.gitProjectId,
                    mrId = event.object_attributes.id.toString(),
                    retry = ApiRequestRetryInfo(true)
                )?.baseInfo
                data.context.gitMrReviewInfo = apiService.getMrReview(
                    cred = getGitCred(),
                    gitProjectId = event.object_attributes.target_project_id.toString(),
                    mrId = event.object_attributes.id.toString(),
                    retry = ApiRequestRetryInfo(true)
                )
            } catch (e: Throwable) {
                logger.warn("TGit MR action cache mrInfo/mrReviewInfo error", e)
            }
        }
    }

    override fun tryGetMrInfoFromCache(): TGitMrInfo? {
        return data.context.gitMrInfo?.let {
            TGitMrInfo(
                mergeStatus = it.mergeStatus ?: "",
                baseCommit = it.baseCommit
            )
        }
    }

    override fun needUpdateLastModifyUser(filePath: String) =
        super.needUpdateLastModifyUser(filePath) && !checkMrForkAction()

    override fun isStreamDeleteAction() = event().isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        val event = event()
        // 目前不支持Mr信息更新的触发
        if (event.object_attributes.action == "update" &&
            event.object_attributes.extension_action != "push-update"
        ) {
            logger.info("TGitMrActionGit|buildRequestEvent|merge request|action|${event.object_attributes.action}")
            return null
        }

        return GitRequestEventHandle.createMergeEvent(event, eventStr)
    }

    override fun skipStream(): Boolean {
        return false
    }

    override fun checkProjectConfig() {
        if (!data.setting.buildPushedPullRequest) {
            throw StreamTriggerException(this, TriggerReason.BUILD_MERGE_REQUEST_DISABLED)
        }
    }

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean {
        // 已合并的无需检查
        if (event().object_attributes.action == TGitMergeActionKind.MERGE.value) {
            return true
        }
        return true
    }

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {
        if (event().isMrForkNotMergeEvent()) {
            return
        }
        val deleteList = mutableListOf<String>()
        val gitMrChangeInfo = apiService.getMrChangeInfo(
            cred = this.getGitCred(),
            gitProjectId = data.eventCommon.gitProjectId,
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
        val targetRef = GitActionCommon.getTriggerBranch(event.object_attributes.target_branch)
        val targetBranchYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = getGitProjectIdOrName(),
            ref = targetRef
        ).toSet()

        // 已经merged的直接返回目标分支的文件列表即可
        if (event.isMrMergeEvent()) {
            return targetBranchYamlPathList.map { (name, blobId) ->
                YamlPathListEntry(name, CheckType.NO_NEED_CHECK, targetRef, blobId)
            }
        }

        // 获取源分支文件列表
        val sourceBranchYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = event.object_attributes.source_project_id.toString(),
            ref = data.eventCommon.commit.commitId,
            cred = if (event.isMrForkEvent()) {
                getForkGitCred()
            } else {
                null
            }
        ).toSet()

        return checkMrYamlPathList(
            sourceBranchYamlPathList = sourceBranchYamlPathList,
            targetBranchYamlPathList = targetBranchYamlPathList,
            changeSet = getChangeSet()!!,
            sourceRef = event.object_attributes.source_branch,
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
        if (event.isMrMergeEvent()) {
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
            gitProjectId = event.object_attributes.target_project_id.toString(),
            fileName = fileName,
            ref = event.object_attributes.target_branch,
            retry = ApiRequestRetryInfo(true)
        )

        if (!getChangeSet()!!.contains(fileName)) {
            return if (targetFile?.content.isNullOrBlank()) {
                logger.warn(
                    "${data.getGitProjectId()} mr request ${data.context.requestEventId}" +
                        "get file $fileName content from ${event.object_attributes.target_project_id} " +
                        "branch ${event.object_attributes.target_branch} is blank because no file"
                )
                MrYamlInfo(
                    event.object_attributes.target_branch, "", targetFile?.blobId
                )
            } else {
                val c = String(Base64.getDecoder().decode(targetFile!!.content))
                if (c.isBlank()) {
                    logger.warn(
                        "${data.getGitProjectId()} mr request ${data.context.requestEventId}" +
                            "get file $fileName content from ${event.object_attributes.target_project_id} " +
                            "target branch ${event.object_attributes.target_branch} is blank " +
                            "because git content blank"
                    )
                }
                MrYamlInfo(event.object_attributes.target_branch, c, targetFile.blobId)
            }
        }

        val sourceFile = getFileInfo(
            cred = if (event.isMrForkEvent()) {
                getForkGitCred()
            } else {
                getGitCred()
            },
            gitProjectId = event.object_attributes.source_project_id.toString(),
            fileName = fileName,
            ref = event.object_attributes.last_commit.id,
            retry = ApiRequestRetryInfo(true)
        )
        val sourceContent = if (sourceFile?.content.isNullOrBlank()) {
            logger.warn(
                "TGitMrActionGit|getYamlContent|no file|projectId|${data.getGitProjectId()}" +
                    "|eventId|${data.context.requestEventId}" +
                    "|file|$fileName|source_project_id|${event.object_attributes.source_project_id} " +
                    "|commit ${event.object_attributes.last_commit.id}"
            )
            // 返回回去的ref目前只用于触发器缓存的逻辑，所以是返回具体分支而不是commit
            MrYamlInfo(event.object_attributes.source_branch, "", sourceFile?.blobId)
        } else {
            val c = String(Base64.getDecoder().decode(sourceFile!!.content))
            if (c.isBlank()) {
                logger.warn(
                    "TGitMrActionGit|getYamlContent|git content blank" +
                        "|projectId|${data.getGitProjectId()}|eventId|${data.context.requestEventId}" +
                        "|file|$fileName|source_project_id|${event.object_attributes.source_project_id} " +
                        "|commit ${event.object_attributes.last_commit.id}"
                )
            }
            MrYamlInfo(event.object_attributes.source_branch, c, sourceFile.blobId)
        }

        if (targetFile?.blobId.isNullOrBlank()) {
            return sourceContent
        }

        if (targetFile?.blobId == sourceFile?.blobId) {
            return sourceContent
        }

        val baseTargetFile = getFileInfo(
            cred = getGitCred(),
            gitProjectId = event.object_attributes.target_project_id.toString(),
            fileName = fileName,
            ref = getMrInfo().baseCommit,
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

    private fun getMrInfo(): StreamMrInfo {
        return if (data.context.mrInfo != null) {
            data.context.mrInfo!!
        } else {
            val mergeRequest = tryGetMrInfoFromCache() ?: apiService.getMrInfo(
                cred = getGitCred(),
                gitProjectId = event().object_attributes.target_project_id.toString(),
                mrId = event().object_attributes.id.toString(),
                retry = ApiRequestRetryInfo(true)
            )!!
            data.context.mrInfo = StreamMrInfo(mergeRequest.baseCommit)
            data.context.mrInfo!!
        }
    }

    override fun getChangeSet(): Set<String>? {
        // 使用null和empty的区别来判断是否调用过获取函数
        if (this.data.context.changeSet != null) {
            return this.data.context.changeSet
        }

        // 获取mr请求的变更文件列表，用来给后面判断
        val changeSet = mutableSetOf<String>()
        apiService.getMrChangeInfo(
            cred = (this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred()) as TGitCred,
            // 获取mr信息的project Id和事件强关联，不一定是流水线所处库
            gitProjectId = data.eventCommon.gitProjectId,
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
    ): TGitFileInfo? {
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
    fun checkMrYamlPathList(
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
            sourceBranch = GitActionCommon.getTriggerBranch(event.object_attributes.source_branch),
            targetBranch = GitActionCommon.getTriggerBranch(event.object_attributes.target_branch),
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

    override fun needSaveOrUpdateBranch() = !event().isMrForkEvent()

    override fun sendUnlockWebhook() {
        if (event().manual_unlock == true) {
            gitCheckService.sendUnlockWebhook(
                gitProjectId = data.getGitProjectId(),
                mrId = event().object_attributes.id,
                // 解锁延迟5s，确保在commit check发送后再发送webhook锁解锁
                delayMills = 5000
            )
        }
    }

    override fun getWebhookCommitList(page: Int, pageSize: Int): List<WebhookCommit> {
        return apiService.getMrCommitList(
            cred = getGitCred(),
            gitUrl = data.setting.gitHttpUrl,
            mrId = event().object_attributes.id,
            page = page,
            pageSize = pageSize,
            retry = ApiRequestRetryInfo(true)
        ).map {
            val commitTime =
                DateTimeUtil.convertDateToLocalDateTime(Date(DateTimeUtil.zoneDateToTimestamp(it.committed_date)))
            WebhookCommit(
                commitId = it.id,
                authorName = it.author_name,
                message = it.message,
                repoType = ScmType.CODE_TGIT.name,
                commitTime = commitTime,
                eventType = CodeEventType.MERGE_REQUEST.name,
                mrId = event().object_attributes.id.toString(),
                action = event().object_attributes.action
            )
        }
    }
}

private fun GitMergeRequestEvent.getActionValue(): String? {
    return when (object_attributes.action) {
        TGitMergeActionKind.OPEN.value -> StreamMrEventAction.OPEN.value
        TGitMergeActionKind.CLOSE.value -> StreamMrEventAction.CLOSE.value
        TGitMergeActionKind.REOPEN.value -> StreamMrEventAction.REOPEN.value
        TGitMergeActionKind.UPDATE.value -> {
            if (object_attributes.extension_action == TGitMergeExtensionActionKind.PUSH_UPDATE.value) {
                StreamMrEventAction.PUSH_UPDATE.value
            } else {
                null
            }
        }
        TGitMergeActionKind.MERGE.value -> StreamMrEventAction.MERGE.value
        else -> null
    }
}
