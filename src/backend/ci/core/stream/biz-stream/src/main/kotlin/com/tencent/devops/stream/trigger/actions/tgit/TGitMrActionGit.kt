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
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitMrActionData
import com.tencent.devops.stream.trigger.actions.tgit.data.TGitMrEventCommonData
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import com.tencent.devops.stream.trigger.parsers.PipelineDelete
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.triggerParameter.GitRequestEventHandle
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.MrCommentBody
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.GitCheckService
import com.tencent.devops.stream.util.QualityUtils
import com.tencent.devops.stream.util.StreamCommonUtils
import org.slf4j.LoggerFactory
import java.util.Base64

class TGitMrActionGit(
    private val apiService: TGitApiService,
    private val mrConflictCheck: MergeConflictCheck,
    private val pipelineDelete: PipelineDelete,
    private val gitCheckService: GitCheckService
) : TGitActionGit(apiService, gitCheckService), StreamMrAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitMrActionGit::class.java)
    }

    override val metaData: ActionMetaData = ActionMetaData(streamObjectKind = StreamObjectKind.MERGE_REQUEST)

    override lateinit var data: ActionData
    fun data() = data as TGitMrActionData

    override val mrIId: String
        get() = data().event.object_attributes.iid.toString()

    override fun addMrComment(body: MrCommentBody) {
        apiService.addMrComment(
            cred = getGitCred(),
            gitProjectId = data.eventCommon.gitProjectId,
            mrId = data().event.object_attributes.id,
            mrBody = QualityUtils.getQualityReport(body.reportData.first, body.reportData.second)
        )
    }

    override val api: TGitApiService
        get() = apiService

    // 获取Fork库的凭证数据
    private fun getForkGitCred() = TGitCred(data().setting.enableUser)

    override fun initCommonData(): GitBaseAction {
        this.data.eventCommon = TGitMrEventCommonData(data().event)
        return this
    }

    override fun isStreamDeleteAction() = data().event.isDeleteEvent()

    override fun buildRequestEvent(eventStr: String): GitRequestEvent? {
        val rData = data()
        // 目前不支持Mr信息更新的触发
        if (rData.event.object_attributes.action == "update" &&
            rData.event.object_attributes.extension_action != "push-update"
        ) {
            logger.info("Git web hook is ${rData.event.object_attributes.action} merge request")
            return null
        }

        return GitRequestEventHandle.createMergeEvent(rData.event, eventStr)
    }

    override fun skipStream(): Boolean {
        return false
    }

    override fun checkProjectConfig() {
        if (!data().setting.buildPushedPullRequest) {
            throw StreamTriggerException(this, TriggerReason.BUILD_MERGE_REQUEST_DISABLED)
        }
    }

    override fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean {
        // 已合并的无需检查
        if (data().event.object_attributes.action != TGitMergeActionKind.MERGE.value) {
            return true
        }
        return mrConflictCheck.checkMrConflict(this, path2PipelineExists)
    }

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {
        if (data().event.isMrForkNotMergeEvent()) {
            return
        }
        val deleteList = mutableListOf<String>()
        val gitMrChangeInfo = apiService.getMrChangeInfo(
            cred = this.getGitCred(),
            gitProjectId = data.eventCommon.gitProjectId,
            mrId = data().event.object_attributes.id.toString(),
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
        val event = data().event
        // 获取目标分支的文件列表
        val targetBranchYamlPathList = TGitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = data.getGitProjectId(),
            ref = TGitActionCommon.getTriggerBranch(event.object_attributes.target_branch)
        ).toSet()

        // 获取mr请求的变更文件列表，用来给后面判断
        val changeSet = mutableSetOf<String>()
        apiService.getMrChangeInfo(
            cred = getGitCred(),
            gitProjectId = data.getGitProjectId(),
            mrId = event.object_attributes.id.toString(),
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

        data.context.changeSet = changeSet.toList()

        // 已经merged的直接返回目标分支的文件列表即可
        if (event.isMrMergeEvent()) {
            return targetBranchYamlPathList.map { YamlPathListEntry(it, CheckType.NO_NEED_CHECK) }
        }

        // 获取源分支文件列表
        val sourceBranchYamlPathList = TGitActionCommon.getYamlPathList(
            this,
            event.object_attributes.source_project_id.toString(),
            ref = data.eventCommon.commit.commitId,
            cred = if (event.isMrForkEvent()) {
                getForkGitCred()
            } else {
                null
            }
        ).toSet()

        return checkMrYamlPathList(sourceBranchYamlPathList, targetBranchYamlPathList, changeSet)
            .map { YamlPathListEntry(it.key, it.value) }
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
    override fun getYamlContent(fileName: String): String {
        val event = data().event
        if (event.isMrMergeEvent()) {
            return api.getFileContent(
                cred = this.getGitCred(),
                gitProjectId = data.getGitProjectId(),
                fileName = fileName,
                ref = data.eventCommon.branch,
                retry = ApiRequestRetryInfo(true)
            )
        }

        val targetFile = apiService.getFileInfo(
            cred = getGitCred(),
            gitProjectId = event.object_attributes.target_project_id.toString(),
            fileName = fileName,
            ref = event.object_attributes.target_branch,
            retry = ApiRequestRetryInfo(true)
        )

        if (!data.context.changeSet!!.contains(fileName)) {
            return if (targetFile?.content.isNullOrBlank()) {
                ""
            } else {
                String(Base64.getDecoder().decode(targetFile!!.content))
            }
        }

        val sourceFile = apiService.getFileInfo(
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
            ""
        } else {
            String(Base64.getDecoder().decode(sourceFile!!.content))
        }

        if (targetFile?.blobId.isNullOrBlank()) {
            return sourceContent
        }

        if (targetFile?.blobId == sourceFile?.blobId) {
            return sourceContent
        }

        val mergeRequest = apiService.getMrInfo(
            cred = getGitCred(),
            gitProjectId = event.object_attributes.target_project_id.toString(),
            mrId = event.object_attributes.id.toString(),
            retry = ApiRequestRetryInfo(true)
        )!!
        val baseTargetFile = apiService.getFileInfo(
            cred = getGitCred(),
            gitProjectId = event.object_attributes.target_project_id.toString(),
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

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val event = data().event
        val mrAction = event.getActionValue() ?: false
        val isMatch = TriggerMatcher.isMrMatch(
            triggerOn = triggerOn,
            sourceBranch = TGitActionCommon.getTriggerBranch(event.object_attributes.source_branch),
            targetBranch = TGitActionCommon.getTriggerBranch(event.object_attributes.target_branch),
            changeSet = data.context.changeSet?.toSet(),
            userId = data.eventCommon.userId,
            mrAction = mrAction
        )
        val params = TGitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
        return TriggerResult(
            trigger = isMatch,
            startParams = params,
            timeTrigger = false,
            deleteTrigger = false
        )
    }

    override fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String> {
        return TGitActionCommon.getStartParams(
            action = this,
            triggerOn = triggerOn
        )
    }

    override fun needSaveOrUpdateBranch() = !data().event.isMrForkEvent()
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
