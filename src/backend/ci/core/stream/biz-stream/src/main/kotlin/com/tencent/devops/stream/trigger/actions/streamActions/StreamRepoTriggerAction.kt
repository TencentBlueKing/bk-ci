package com.tencent.devops.stream.trigger.actions.streamActions

import com.tencent.devops.common.client.Client
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.tgit.TGitActionCommon
import com.tencent.devops.stream.trigger.actions.tgit.TGitActionGit
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.util.CommonCredentialUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType

@Suppress("ALL")
class StreamRepoTriggerAction(
    // 可能会包含stream action事件类似删除
    private val baseAction: BaseAction,
    private val client: Client
) : BaseAction {
    override val metaData: ActionMetaData = baseAction.metaData
    override var data: ActionData = baseAction.data
    override val api: StreamGitApiService = baseAction.api

    override fun init(): BaseAction? {
        baseAction.init()
        return this
    }

    override fun getProjectCode(gitProjectId: String?) = baseAction.getProjectCode(gitProjectId)

    override fun getGitCred(personToken: String?): StreamGitCred = baseAction.getGitCred(personToken)

    override fun buildRequestEvent(eventStr: String) = baseAction.buildRequestEvent(eventStr)

    override fun skipStream() = baseAction.skipStream()

    override fun checkProjectConfig() {
        baseAction.checkProjectConfig()
    }

    override fun checkMrConflict(
        path2PipelineExists: Map<String, StreamTriggerPipeline>
    ) = baseAction.checkMrConflict(path2PipelineExists)

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {}

    override fun getYamlPathList(): List<YamlPathListEntry> {
        return TGitActionCommon.getYamlPathList(
            action = baseAction,
            gitProjectId = data.getGitProjectId(),
            ref = data.context.repoTrigger!!.branch
        ).map { YamlPathListEntry(it, CheckType.NO_NEED_CHECK) }
    }

    override fun getYamlContent(fileName: String): String {
        return api.getFileContent(
            cred = baseAction.getGitCred(),
            gitProjectId = data.getGitProjectId(),
            fileName = fileName,
            ref = data.context.repoTrigger!!.branch,
            retry = ApiRequestRetryInfo(true)
        )
    }

    override fun getChangeSet(): Set<String>? {
        return baseAction.getChangeSet()
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val branch = TGitActionCommon.getTriggerBranch(data.eventCommon.branch)

        val isDefaultBranch = branch == data.context.defaultBranch
        // 校验是否注册跨项目触发
        if (isDefaultBranch) {
            triggerCheckRepoTriggerCredentials(triggerOn)
        }
        return baseAction.isMatch(triggerOn)
    }

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? = null

    override fun needSaveOrUpdateBranch() = false

    override fun needSendCommitCheck() = baseAction.needSendCommitCheck()

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
        baseAction.sendCommitCheck(buildId, gitProjectName, state, block, context, targetUrl, description)
    }

    override fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook) {
        val (_, userName) = checkRepoTriggerCredentials(
            repoHook = repoHook
        )
        // 表示路径至少为2级，不支持只填一级路径进行模糊匹配
        if (repoHook.name!!.contains("/") && !repoHook.name!!.startsWith("/")) {
            checkHaveGroupName(repoHook.name!!, userName)
        }

    }

    override fun updateLastBranch(pipelineId: String, branch: String) {
        baseAction.updateLastBranch(pipelineId, branch)
    }

    private fun checkHaveGroupName(
        name: String,
        userName: String?
    ) {
        val firstGroupName = name.split("/").firstOrNull()
        baseAction.api.getProjectList(
            cred = TGitCred(userId = userName),
            search = firstGroupName,
            minAccessLevel = GitAccessLevelEnum.MASTER
        )?.ifEmpty { null } ?: throw StreamTriggerException(
            action = this,
            triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
            reasonParams = listOf("First level group[$firstGroupName] does not exist"),
            commitCheck = CommitCheck(
                block = false,
                state = StreamCommitCheckState.FAILURE
            )
        )
    }

    /**
     * 判断是否可以注册跨项目构建事件
     * @return 用户名称
     */
    private fun triggerCheckRepoTriggerCredentials(triggerOn: TriggerOn): String? {
        if (triggerOn.repoHook == null) {
            return null
        }
        val (repoTriggerCredentialsCheck, repoTriggerUserId) = checkRepoTriggerCredentials(triggerOn.repoHook!!)
        if (!repoTriggerCredentialsCheck) {
            throw StreamTriggerException(
                action = this,
                triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                reasonParams = listOf(
                    "Permissions denied, master and above permissions are required. " +
                        "Repo: (${triggerOn.repoHook?.name})"
                ),
                commitCheck = CommitCheck(
                    block = false,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }
        // 增加远程仓库时所使用权限的userId
        this.data.context.repoTrigger = this.data.context.repoTrigger?.copy(buildUserID = repoTriggerUserId)
        return repoTriggerUserId
    }

    private fun checkRepoTriggerCredentials(repoHook: RepositoryHook): Pair<Boolean, String?> {
        val token = when {
            repoHook.credentialsForTicketId != null ->
                try {
                    CommonCredentialUtils.getCredential(
                        client = client,
                        projectId = "git_${this.data.getGitProjectId()}",
                        credentialId = repoHook.credentialsForTicketId!!,
                        type = CredentialType.ACCESSTOKEN
                    )["v1"] ?: return Pair(false, null)
                } catch (e: Throwable) {
                    throw StreamTriggerException(
                        action = this,
                        triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                        reasonParams = listOf("Credential [${repoHook.credentialsForTicketId}] does not exist"),
                        commitCheck = CommitCheck(
                            block = false,
                            state = StreamCommitCheckState.FAILURE
                        )
                    )
                }
            repoHook.credentialsForToken != null -> repoHook.credentialsForToken!!
            else -> throw StreamTriggerException(
                action = this,
                triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                reasonParams = listOf("credentials cannot be null"),
                commitCheck = CommitCheck(
                    block = false,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }
        // stream 侧需要的是user 数字id 而不是 rtx
        val userInfo = try {
            this.api.getUserInfoByToken(
                TGitCred(
                    userId = null,
                    accessToken = token,
                    useAccessToken = false
                )
            ) ?: return Pair(false, null)
        } catch (e: Throwable) {
            throw StreamTriggerException(
                action = this,
                triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                reasonParams = listOf("401 Unauthorized. Repo:(${repoHook.name})"),
                commitCheck = CommitCheck(
                    block = false,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }
        val check = this.api.getProjectUserInfo(
            cred = TGitCred(
                userId = null,
                accessToken = token,
                useAccessToken = false
            ),
            userId = userInfo.id,
            gitProjectId = this.data.eventCommon.gitProjectId
        ).accessLevel >= 40
        return Pair(check, userInfo.username)
    }
}
