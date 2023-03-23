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
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitCommit
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteBranch
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.DeleteRule
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.on.check
import com.tencent.devops.scm.pojo.WebhookCommit
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
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.PathMatchUtils
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
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
import java.util.Date

@Suppress("ALL")
class TGitPushActionGit(
    private val dslContext: DSLContext,
    private val apiService: TGitApiService,
    private val streamEventService: StreamEventService,
    private val streamTimerService: StreamTimerService,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamDeleteEventService: DeleteEventService,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService
) : TGitActionGit(apiService, gitCheckService), GitBaseAction {

    companion object {
        val logger = LoggerFactory.getLogger(TGitPushActionGit::class.java)
        val SKIP_CI_KEYS = setOf("skip ci", "ci skip", "no ci", "ci.skip")
        private const val PUSH_OPTIONS_PREFIX = "ci.variable::"
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.PUSH)

    override fun event() = data.event as GitPushEvent

    override val api: TGitApiService
        get() = apiService

    override fun init(): BaseAction? {
        return initCommonData()
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        val lastCommit = getLatestCommit(event)
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.project_id.toString(),
            scmType = ScmType.CODE_GIT,
            branch = event.ref.removePrefix("refs/heads/"),
            commit = EventCommonDataCommit(
                commitId = event.after,
                commitMsg = lastCommit?.message,
                commitTimeStamp = GitActionCommon.getCommitTimeStamp(lastCommit?.timestamp),
                commitAuthorName = lastCommit?.author?.name
            ),
            userId = event.user_name,
            gitProjectName = GitUtils.getProjectName(event.repository.homepage)
        )
        return this
    }

    private fun getLatestCommit(
        event: GitPushEvent
    ): GitCommit? {
        if (event.isDeleteEvent()) {
            return null
        }
        val commitId = event.after
        val commits = event.commits
        if (commitId == null) {
            return if (commits.isNullOrEmpty()) {
                null
            } else {
                commits.last()
            }
        }
        commits?.forEach {
            if (it.id == commitId) {
                return it
            }
        }
        return null
    }

    override fun isStreamDeleteAction() = event().isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        if (!event().pushEventFilter()) {
            return null
        }
        return GitRequestEventHandle.createPushEvent(event(), eventStr)
    }

    override fun skipStream(): Boolean {
        if (!event().skipStream()) {
            return false
        }
        logger.info(
            "TGitPushActionGit|skipStream" +
                "|project|${data.eventCommon.gitProjectId}|commit|${data.eventCommon.commit.commitId}"
        )
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
        if (event().isDeleteBranch()) {
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

        val deleteYamlFiles = event().commits?.flatMap {
            if (it.removed != null) {
                it.removed!!.asIterable()
            } else {
                emptyList()
            }
        }?.filter { StreamCommonUtils.isCiFile(it) }
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

    override fun checkIfModify() = true

    override fun getChangeSet(): Set<String>? {
        // 使用null和empty的区别来判断是否调用过获取函数
        if (this.data.context.changeSet != null) {
            return this.data.context.changeSet
        }

        val gitEvent = event()
        if (gitEvent.create_and_update != null) {
            return getSpecialChangeSet(gitEvent)
        }
        val changeSet = mutableSetOf<String>()

        // git push -f 使用反向进行三点比较可以比较出rebase的真实提交
        val from = if (gitEvent.operation_kind == TGitPushOperationKind.UPDATE_NONFASTFORWORD.value) {
            gitEvent.after
        } else {
            gitEvent.before
        }

        val to = if (gitEvent.operation_kind == TGitPushOperationKind.UPDATE_NONFASTFORWORD.value) {
            gitEvent.before
        } else {
            gitEvent.after
        }

        for (i in 1..10) {
            val result = apiService.getCommitChangeList(
                cred = (this.data.context.repoTrigger?.repoTriggerCred ?: getGitCred()) as TGitCred,
                gitProjectId = data.eventCommon.gitProjectId,
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

    private fun getSpecialChangeSet(gitEvent: GitPushEvent): Set<String> {
        // 为 false 时表示为纯创建分支
        if (gitEvent.create_and_update == false) return mutableSetOf()
        val changeSet = mutableSetOf<String>()
        gitEvent.commits?.forEach {
            changeSet.addAll(it.removed.orEmpty())
            changeSet.addAll(it.modified.orEmpty())
            changeSet.addAll(it.added.orEmpty())
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
            checkCreateAndUpdate = event().create_and_update
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
            logger.info("updateLastBranch fail,pipelineId:$pipelineId,branch:$branch,")
        }
    }
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
        return replaceVariablesByPushOptions(yamlVariables, event().push_options)
    }

    // git push -o ci.variable::<name>="<value>" -o ci.variable::<name>="<value>"
    private fun replaceVariablesByPushOptions(
        variables: Map<String, Variable>?,
        pushOptions: Map<String, String>?
    ): Map<String, Variable>? {
        if (variables.isNullOrEmpty() || pushOptions.isNullOrEmpty()) {
            return variables
        }
        val variablesOptionsKeys = pushOptions.keys.filter { it.startsWith(PUSH_OPTIONS_PREFIX) }
            .map { it.removePrefix(PUSH_OPTIONS_PREFIX) }

        val result = variables.toMutableMap()
        variables.forEach { (key, value) ->
            // 不替换只读变量
            if (value.readonly != null && value.readonly == true) {
                return@forEach
            }
            if (key in variablesOptionsKeys) {
                result[key] = Variable(
                    value = pushOptions["${PUSH_OPTIONS_PREFIX}$key"],
                    readonly = value.readonly
                )
            }
        }
        return result
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return GitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
    }

    override fun needSaveOrUpdateBranch() = true

    override fun needSendCommitCheck() = !event().isDeleteBranch()

    override fun getWebhookCommitList(page: Int, pageSize: Int): List<WebhookCommit> {
        if (page > 1) {
            // push 请求事件会在第一次请求时将所有的commit记录全部返回，所以如果分页参数不为1，则直接返回空列表
            return emptyList()
        }
        return event().commits!!.map {
            val commitTime =
                DateTimeUtil.convertDateToLocalDateTime(Date(DateTimeUtil.zoneDateToTimestamp(it.timestamp)))
            WebhookCommit(
                commitId = it.id,
                authorName = it.author.name,
                message = it.message,
                repoType = ScmType.CODE_TGIT.name,
                commitTime = commitTime,
                eventType = CodeEventType.PUSH.name,
                mrId = null,
                action = event().action_kind
            )
        }
    }
}

@SuppressWarnings("ReturnCount")
private fun GitPushEvent.pushEventFilter(): Boolean {
    // 放开删除分支操作为了流水线删除功能
    if (isDeleteBranch()) {
        return true
    }
    if (total_commits_count <= 0) {
        TGitPushActionGit.logger.info("$checkout_sha Git push web hook no commit($total_commits_count)")
        return false
    }
    if (GitUtils.isPrePushBranch(ref)) {
        TGitPushActionGit.logger.info("Git web hook is pre-push event|branchName=$ref")
        return false
    }
    return true
}

private fun GitPushEvent.skipStream(): Boolean {
    // 判断commitMsg
    commits?.filter { it.id == after }?.forEach { commit ->
        TGitPushActionGit.SKIP_CI_KEYS.forEach { key ->
            if (commit.message.contains(key)) {
                return true
            }
        }
    }
    push_options?.keys?.forEach {
        if (it == "ci.skip") {
            return true
        }
    }
    return false
}
