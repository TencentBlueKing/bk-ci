/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 *
 */

package com.tencent.devops.process.yaml.actions.tgit

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.isMrForkEvent
import com.tencent.devops.common.webhook.pojo.code.git.isMrMergeEvent
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.yaml.PipelineYamlService
import com.tencent.devops.process.yaml.actions.BaseAction
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.actions.GitBaseAction
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.actions.data.EventCommonData
import com.tencent.devops.process.yaml.actions.data.EventCommonDataCommit
import com.tencent.devops.process.yaml.exception.YamlTriggerException
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitCred
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitFileInfo
import com.tencent.devops.process.yaml.git.service.TGitApiService
import com.tencent.devops.process.yaml.pojo.CheckType
import com.tencent.devops.process.yaml.pojo.MrYamlInfo
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.slf4j.LoggerFactory
import java.util.Base64

class TGitMrActionGit(
    private val apiService: TGitApiService,
    private val pipelineYamlService: PipelineYamlService
) : TGitActionGit(apiService), GitBaseAction {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitMrActionGit::class.java)
    }

    override val metaData = ActionMetaData(streamObjectKind = StreamObjectKind.MERGE_REQUEST)

    override fun event() = data.event as GitMergeRequestEvent

    override val api: TGitApiService
        get() = apiService

    override fun init(): BaseAction {
        return initCommonData()
    }

    // 获取Fork库的凭证数据
    private fun getForkGitCred(): TGitCred {
        return TGitCred(event().user.username)
    }

    private fun initCommonData(): GitBaseAction {
        val event = event()
        this.data.eventCommon = EventCommonData(
            gitProjectId = event.object_attributes.target_project_id.toString(),
            scmType = ScmType.CODE_GIT,
            sourceGitProjectId = event.object_attributes.source_project_id.toString(),
            sourceGitNamespace = event.object_attributes.source.namespace,
            fork = event.isMrForkEvent(),
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
            projectName = GitUtils.getProjectName(event.object_attributes.target.http_url)
        )
        return this
    }

    override fun initCacheData() {
        val event = event()
        try {
            val gitProjectId = event.object_attributes.target_project_id.toString()

            val gitProjectInfo = apiService.getGitProjectInfo(
                cred = this.getGitCred(),
                gitProjectId = gitProjectId,
                retry = ApiRequestRetryInfo(true)
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_GIT_PROJECT_NOT_FOUND_OR_NOT_PERMISSION,
                params = arrayOf(gitProjectId)
            )
            val defaultBranch = gitProjectInfo.defaultBranch!!
            data.context.defaultBranch = defaultBranch
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

    override fun getYamlPathList(): List<YamlPathListEntry> {
        val event = event()
        // 获取目标分支的文件列表
        val targetRef =
            GitActionCommon.getTriggerBranch(event.object_attributes.target_branch)
        val targetBranchYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = getGitProjectIdOrName(),
            ref = targetRef
        ).toSet()

        // 已经merged的直接返回目标分支的文件列表即可
        if (event.isMrMergeEvent()) {
            val yamlPathFiles = mutableListOf<YamlPathListEntry>()
            val targetBranchFiles = targetBranchYamlPathList.map { (name, blobId) ->
                YamlPathListEntry(
                    name,
                    CheckType.NO_NEED_CHECK,
                    targetRef,
                    blobId
                )
            }
            yamlPathFiles.addAll(targetBranchFiles)
            // 如果分支已经合入默认分支,则需要将源分支产生的分支版本删除
            if (event.object_attributes.target_branch == data.context.defaultBranch) {
                val ref = GitActionCommon.getRealRef(action = this, branch = event.object_attributes.source_branch)
                val sourceBranchFiles = pipelineYamlService.getAllBranchFilePath(
                    projectId = data.setting.projectId,
                    repoHashId = data.setting.repoHashId,
                    branch = ref
                ).map { filePath ->
                    YamlPathListEntry(
                        filePath,
                        CheckType.MERGED,
                        ref,
                        null
                    )
                }
                yamlPathFiles.addAll(sourceBranchFiles)
            }
            return yamlPathFiles
        }

        // 获取源分支文件列表
        val sourceBranchYamlPathList = GitActionCommon.getYamlPathList(
            action = this,
            gitProjectId = event.object_attributes.source_project_id.toString(),
            ref = event.object_attributes.last_commit.id,
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
                    "${data.setting.gitProjectId} mr request" +
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
                        "${data.setting.gitProjectId} mr request" +
                            "get file $fileName content from ${event.object_attributes.target_project_id} " +
                            "target branch ${event.object_attributes.target_branch} is blank " +
                            "because git content blank"
                    )
                }
                MrYamlInfo(
                    event.object_attributes.target_branch,
                    c,
                    targetFile.blobId
                )
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
                "TGitMrActionGit|getYamlContent|no file|projectId|${data.setting.projectId}" +
                    "|file|$fileName|source_project_id|${event.object_attributes.source_project_id} " +
                    "|commit ${event.object_attributes.last_commit.id}"
            )
            // 返回回去的ref目前只用于触发器缓存的逻辑，所以是返回具体分支而不是commit
            MrYamlInfo(
                event.object_attributes.source_branch,
                "",
                sourceFile?.blobId
            )
        } else {
            val c = String(Base64.getDecoder().decode(sourceFile!!.content))
            if (c.isBlank()) {
                logger.warn(
                    "TGitMrActionGit|getYamlContent|git content blank" +
                        "|projectId|${data.setting.projectId}}|" +
                        "|file|$fileName|source_project_id|${event.object_attributes.source_project_id} " +
                        "|commit ${event.object_attributes.last_commit.id}"
                )
            }
            MrYamlInfo(
                event.object_attributes.source_branch,
                c,
                sourceFile.blobId
            )
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
        throw YamlTriggerException(
            action = this,
            reason = PipelineTriggerReason.TRIGGER_FAILED,
            errorCode = ProcessMessageCode.ERROR_CI_YAML_NEED_MERGE_OR_REBASE,
            params = arrayOf(fileName)
        )
    }

    private fun getMrInfo(): GitMrInfo {
        return if (data.context.gitMrInfo != null) {
            data.context.gitMrInfo!!
        } else {
            val gitMrInfo = apiService.getMrInfo(
                cred = getGitCred(),
                gitProjectId = event().object_attributes.target_project_id.toString(),
                mrId = event().object_attributes.id.toString(),
                retry = ApiRequestRetryInfo(true)
            )!!
            data.context.gitMrInfo = gitMrInfo.baseInfo
            data.context.gitMrInfo!!
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
            cred = getGitCred(),
            // 获取mr信息的project Id和事件强关联，不一定是流水线所处库
            gitProjectId = data.eventCommon.gitProjectId,
            mrId = event().object_attributes.id.toString(),
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
        cred: TGitCred,
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
    private fun checkMrYamlPathList(
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
                // 源分支有，目标分支没有，变更列表有，以源分支为主，需要校验版本
                source !in targetList && source in changeSet -> {
                    result.add(
                        YamlPathListEntry(
                            source,
                            CheckType.NEED_CHECK,
                            sourceRef,
                            blobId
                        )
                    )
                }
                // 源分支有，目标分支没有，变更列表没有，不触发且提示错误
                source !in targetList && source !in changeSet -> {
                    result.add(
                        YamlPathListEntry(
                            source,
                            CheckType.NO_TRIGGER,
                            sourceRef,
                            blobId
                        )
                    )
                }
                // 源分支有，目标分支有，变更列表有，需要校验版本
                source in targetList && source in changeSet -> {
                    result.add(
                        YamlPathListEntry(
                            source,
                            CheckType.NEED_CHECK,
                            sourceRef,
                            blobId
                        )
                    )
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
                    result.add(
                        YamlPathListEntry(
                            target,
                            CheckType.NO_NEED_CHECK,
                            targetRef,
                            blobId
                        )
                    )
                }
                // 源分支有，目标分支有，变更列表无，以目标分支为主，不需要校验版本
                target in sourceList && target !in changeSet -> {
                    result.add(
                        YamlPathListEntry(
                            target,
                            CheckType.NO_NEED_CHECK,
                            targetRef,
                            blobId
                        )
                    )
                }
            }
        }
        return result
    }
}
