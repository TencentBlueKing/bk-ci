package com.tencent.devops.stream.trigger.actions.tgit

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.TGitApiService
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.pojo.enums.toGitState
import com.tencent.devops.stream.trigger.service.GitCheckService

/**
 * 对于stream 平台级功能的具体实现，不需要下放到具体的event
 * 对于只有一两个事件实现的，也可在平台级实现一个通用的，一两个再自己重写
 */
abstract class TGitActionGit(
    override val api: TGitApiService,
    private val gitCheckService: GitCheckService
) : GitBaseAction {
    override lateinit var data: ActionData

    override fun getProjectCode(gitProjectId: String?): String {
        return if (gitProjectId != null) {
            "git_$gitProjectId"
        } else {
            "git_${data.getGitProjectId()}"
        }
    }

    /**
     * 提供拿取gitProjectId的公共方法
     * 因为会存在跨库触发导致的event的gitProjectId和触发的不一致的问题
     * 所以会优先拿取pipeline的gitProjectId
     */
    override fun getGitProjectIdOrName(gitProjectId: String?) =
        gitProjectId ?: data.context.pipeline?.gitProjectId ?: data.eventCommon.gitProjectId

    override fun getGitCred(personToken: String?): TGitCred {
        if (personToken != null) {
            return TGitCred(
                userId = null,
                accessToken = personToken,
                useAccessToken = false
            )
        }
        return TGitCred(data.setting.enableUser)
    }

    override fun getChangeSet(): Set<String>? = null

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? = null

    override fun needSaveOrUpdateBranch() = false

    override fun needSendCommitCheck() = true

    override fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook) = Unit

    override fun sendCommitCheck(
        buildId: String,
        gitProjectName: String,
        state: StreamCommitCheckState,
        block: Boolean,
        context: String,
        targetUrl: String,
        description: String,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>
    ) {
        gitCheckService.pushCommitCheck(
            userId = data.getUserId(),
            projectCode = getProjectCode(),
            buildId = buildId,
            gitProjectId = data.eventCommon.gitProjectId,
            gitProjectName = gitProjectName,
            pipelineId = data.context.pipeline!!.pipelineId,
            commitId = data.eventCommon.commit.commitId,
            gitHttpUrl = data.setting.gitHttpUrl,
            scmType = ScmType.CODE_GIT,
            token = api.getToken(getGitCred()),
            state = state.toGitState(ScmType.CODE_GIT),
            block = block,
            context = context,
            targetUrl = targetUrl,
            description = description,
            mrId = if (data.event is GitMergeRequestEvent) {
                (data.event as GitMergeRequestEvent).object_attributes.iid
            } else {
                null
            },
            manualUnlock = if (data.event is GitMergeRequestEvent) {
                (data.event as GitMergeRequestEvent).manual_unlock
            } else {
                false
            },
            reportData = reportData,
            addCommitCheck = api::addCommitCheck
        )
    }

    override fun updatePipelineLastBranchAndDisplayName(
        pipelineId: String,
        branch: String?,
        displayName: String?
    ) = Unit

    override fun parseStreamTriggerContext(cred: StreamGitCred?) {
        // 格式化repoCreatedTime
        this.data.context.repoCreatedTime = DateTimeUtil.formatDate(
            DateTimeUtil.zoneDateToDate(this.data.context.repoCreatedTime)!!
        )

        // 将repoCreatorId -> user name
        this.data.context.repoCreatorId = this.data.context.repoCreatorId?.let {
            kotlin.runCatching {
                api.getUserInfoById(
                    (cred as TGitCred?) ?: getGitCred(),
                    it
                ).username
            }.getOrNull() ?: ""
        }
    }
}
