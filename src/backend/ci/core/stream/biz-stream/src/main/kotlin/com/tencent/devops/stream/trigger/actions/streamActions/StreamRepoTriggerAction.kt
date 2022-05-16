package com.tencent.devops.stream.trigger.actions.streamActions

import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.tgit.TGitActionCommon
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState

@Suppress("ALL")
class StreamRepoTriggerAction(
    // 可能会包含stream action事件类似删除
    private val baseAction: BaseAction
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

    override fun isMatch(triggerOn: TriggerOn) = baseAction.isMatch(triggerOn)

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
        baseAction.registerCheckRepoTriggerCredentials(repoHook)
    }
}
