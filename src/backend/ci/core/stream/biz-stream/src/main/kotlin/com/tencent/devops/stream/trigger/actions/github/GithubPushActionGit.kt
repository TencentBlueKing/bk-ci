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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.code.github.GithubCommit
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.checkCreateAndUpdate
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.DeleteRule
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.on.check
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.EventCommonData
import com.tencent.devops.stream.trigger.actions.data.EventCommonDataCommit
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.tgit.TGitPushActionGit
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.github.GithubCred
import com.tencent.devops.stream.trigger.git.service.GithubApiService
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.PathMatchUtils
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GithubRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.service.DeleteEventService
import com.tencent.devops.stream.trigger.service.GitCheckService
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import com.tencent.devops.stream.util.StreamCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("ALL")
class GithubPushActionGit(
    private val dslContext: DSLContext,
    private val client: Client,
    private val apiService: GithubApiService,
    private val streamEventService: StreamEventService,
    private val streamTimerService: StreamTimerService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamDeleteEventService: DeleteEventService,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService
) : GithubActionGit(apiService, gitCheckService), GitBaseAction {

    companion object {
        val logger = LoggerFactory.getLogger(GithubPushActionGit::class.java)
        val SKIP_CI_KEYS = setOf("skip ci", "ci skip", "no ci", "ci.skip")
        private const val PUSH_OPTIONS_PREFIX = "ci.variable::"
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.PUSH)

    override fun event() = data.event as GithubPushEvent

    override val api: GithubApiService
        get() = apiService

    override fun init(): BaseAction? {
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val lastCommit = getLatestCommit(event)
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.repository.id.toString(),
            scmType = ScmType.GITHUB,
            branch = event.ref.removePrefix("refs/heads/"),
            commit = EventCommonDataCommit(
                commitId = event.after,
                commitMsg = lastCommit?.message,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(lastCommit?.timestamp),
                commitAuthorName = lastCommit?.author?.name
            ),
            userId = event.sender.login,
            gitProjectName = event.repository.fullName,
            eventType = GithubPushEvent.classType
        )
        return this
    }

    private fun getLatestCommit(
        event: GithubPushEvent
    ): GithubCommit? {
        if (event.deleted) {
            return null
        }
        val commitId = event.after
        val commits = event.commits
        commits.forEach {
            if (it.id == commitId) {
                return it
            }
        }
        return null
    }

    override fun isStreamDeleteAction() = event().deleted

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        if (!event().pushEventFilter()) {
            return null
        }
        return GithubRequestEventHandle.createPushEvent(event(), eventStr)
    }

    override fun skipStream(): Boolean {
        if (!event().skipStream()) {
            return false
        }
        logger.info("project: ${data.eventCommon.gitProjectId} commit: ${data.eventCommon.commit.commitId} skip ci")
        streamEventService.saveTriggerNotBuildEvent(
            action = this,
            reason = TriggerReason.USER_SKIPED.name,
            reasonDetail = TriggerReason.USER_SKIPED.detail
        )
        return true
    }

    override fun checkProjectConfig() {
        if (!data.setting.buildPushedBranches) {
            throw StreamTriggerException(this, TriggerReason.BUILD_PUSHED_BRANCHES_DISABLED)
        }
    }

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean {
        return true
    }

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {
        // 直接删除分支,挪到前面，不需要对deleteYamlFiles获取后再做判断。
        if (event().deleted == true) {
            val pipelines = streamPipelineBranchService.getBranchPipelines(
                this.data.getGitProjectId().toLong(),
                this.data.eventCommon.branch
            )
            pipelines.forEach {
                // 这里需增加获取pipeline对应的yml路径，需要判断该文件是否在默认分支存在。
                val gitPipelineResourceRecord = gitPipelineResourceDao.getPipelineById(
                    dslContext = dslContext,
                    gitProjectId = this.data.getGitProjectId().toLong(),
                    pipelineId = it
                )
                pipelineDelete.delete(
                    action = this,
                    gitProjectId = gitPipelineResourceRecord?.gitProjectId.toString(),
                    pipelineId = it,
                    filePath = gitPipelineResourceRecord?.filePath
                )
            }
            return
        }

        val deleteYamlFiles = event().commits.flatMap {
            it.removed.asIterable()
        }.filter { StreamCommonUtils.isCiFile(it) }
        pipelineDelete.checkAndDeletePipeline(this, path2PipelineExists, deleteYamlFiles)
    }

    override fun getYamlPathList(): List<YamlPathListEntry> {
        val changeSet = getChangeSet()
        return GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = this.getGitProjectIdOrName(),
            ref = this.data.eventCommon.branch
        ).map { (name, blobId) ->
            YamlPathListEntry(
                yamlPath = name,
                checkType = if (changeSet?.contains(name) == true) {
                    CheckType.NEED_CHECK
                } else {
                    CheckType.NO_NEED_CHECK
                },
                ref = this.data.eventCommon.branch, blobId = blobId
            )
        }
    }

    override fun getYamlContent(fileName: String): YamlContent {
        return YamlContent(
            ref = data.eventCommon.branch,
            content = api.getFileContent(
                cred = this.getGitCred(),
                gitProjectId = getGitProjectIdOrName(),
                fileName = fileName,
                ref = data.eventCommon.branch,
                retry = ApiRequestRetryInfo(true)
            )
        )
    }

    override fun getChangeSet(): Set<String>? {
        // 使用null和empty的区别来判断是否调用过获取函数
        if (this.data.context.changeSet != null) {
            return this.data.context.changeSet
        }

        val gitEvent = event()
        if (gitEvent.created == true) {
            return getSpecialChangeSet(gitEvent)
        }
        val changeSet = mutableSetOf<String>()

        // git push -f 使用反向进行三点比较可以比较出rebase的真实提交
        val from = if (gitEvent.forced == true) {
            gitEvent.after
        } else {
            gitEvent.before
        }

        val to = if (gitEvent.forced == true) {
            gitEvent.before
        } else {
            gitEvent.after
        }

        for (i in 1..10) {
            val result = apiService.getCommitChangeList(
                cred = (this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred()) as GithubCred,
                gitProjectId = getGitProjectIdOrName(data.eventCommon.gitProjectId),
                from = from,
                to = to,
                straight = false,
                page = i,
                pageSize = 100,
                retry = ApiRequestRetryInfo(true)
            )
            changeSet.addAll(
                result.map {
                    if (it.deletedFile) {
                        it.oldPath
                    } else if (it.renameFile) {
                        it.oldPath
                        it.newPath
                    } else {
                        it.newPath
                    }
                }
            )
            if (result.size < 100) {
                break
            }
        }

        this.data.context.changeSet = changeSet
        return this.data.context.changeSet
    }

    private fun getSpecialChangeSet(gitEvent: GithubPushEvent): Set<String> {
        // github commits为空表示只创建了分支
        if (gitEvent.commits.isEmpty()) return mutableSetOf()
        val changeSet = mutableSetOf<String>()
        gitEvent.commits.forEach {
            changeSet.addAll(it.removed)
            changeSet.addAll(it.modified)
            changeSet.addAll(it.added)
        }
        return changeSet
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val branch = GitActionCommon.getTriggerBranch(data.eventCommon.branch)

        val isDefaultBranch = branch == data.context.defaultBranch

        // 判断是否注册定时任务
        val isTime = if (isDefaultBranch) {
            isSchedulesMatch(
                triggerOn = triggerOn,
                eventBranch = data.eventCommon.branch,
                userId = data.getUserId(),
                pipelineId = data.context.pipeline!!.pipelineId
            )
        } else {
            false
        }

        // 判断是否注册删除任务
        val changeSet = getChangeSet()
        val isDelete = if (isDefaultBranch) {
            // 只有更改了delete相关流水线才做更新
            PathMatchUtils.isIncludePathMatch(listOf(data.context.pipeline!!.filePath), changeSet) &&
                isDeleteMatch(triggerOn.delete, data.context.pipeline!!.pipelineId)
        } else {
            false
        }

        val isMatch = TriggerMatcher.isPushMatch(
            triggerOn = triggerOn,
            eventBranch = data.eventCommon.branch,
            changeSet = changeSet,
            userId = data.eventCommon.userId,
            checkCreateAndUpdate = event().checkCreateAndUpdate()
        )
        return TriggerResult(
            trigger = isMatch,
            triggerOn = triggerOn,
            timeTrigger = isTime,
            deleteTrigger = isDelete
        )
    }

    override fun updatePipelineLastBranchAndDisplayName(
        pipelineId: String,
        branch: String?,
        displayName: String?
    ) {
        try {
            gitPipelineResourceDao.updatePipelineLastBranchAndDisplayName(
                dslContext = dslContext,
                pipelineId = pipelineId,
                branch = branch,
                displayName = displayName
            )
        } catch (e: Exception) {
            TGitPushActionGit.logger.info("updateLastBranch fail,pipelineId:$pipelineId,branch:$branch,")
        }
    }

    override fun checkIfModify() = true

    // 判断是否注册定时任务来看是修改还是删除
    private fun isSchedulesMatch(
        triggerOn: TriggerOn,
        eventBranch: String,
        userId: String,
        pipelineId: String
    ): Boolean {
        if (triggerOn.schedules == null) {
            // 新流水线没有定时任务就没注册过定时任务
            if (pipelineId.isBlank()) {
                return false
            } else {
                // 不是新流水线的可能注册过了要删除
                streamTimerService.get(pipelineId) ?: return false
                streamTimerService.deleteTimer(pipelineId, userId)
                return false
            }
        } else {
            if (triggerOn.schedules?.cron.isNullOrBlank()) {
                logger.info("The schedules cron is invalid($eventBranch)")
                return false
            }
        }
        return true
    }

    // 判断是否注册默认分支的删除任务
    private fun isDeleteMatch(
        deleteRule: DeleteRule?,
        pipelineId: String
    ): Boolean {
        if (deleteRule == null) {
            if (pipelineId.isBlank()) {
                return false
            } else {
                streamDeleteEventService.getDeleteEvent(pipelineId) ?: return false
                streamDeleteEventService.deleteDeleteEvent(pipelineId)
                return false
            }
        } else {
            if (deleteRule.types.isEmpty() || !deleteRule.check()) {
                return false
            }
        }
        return true
    }

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? {
        return yamlVariables
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return GitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
    }

    override fun needSaveOrUpdateBranch() = true

    override fun needSendCommitCheck() = !event().deleted
}

@SuppressWarnings("ReturnCount")
private fun GithubPushEvent.pushEventFilter(): Boolean {
    // 放开删除分支操作为了流水线删除功能
    if (deleted) {
        return true
    }
//    if (commits.isEmpty()) {
//        GithubPushActionGit.logger.info("$after Github push web hook no commit")
//        return false
//    }
    if (GitUtils.isPrePushBranch(ref)) {
        GithubPushActionGit.logger.info("Github web hook is pre-push event|branchName=$ref")
        return false
    }
    return true
}

private fun GithubPushEvent.skipStream(): Boolean {
    // 判断commitMsg
    commits.filter { it.id == after }.forEach { commit ->
        GithubPushActionGit.SKIP_CI_KEYS.forEach { key ->
            if (commit.message.contains(key)) {
                return true
            }
        }
    }
    return false
}
