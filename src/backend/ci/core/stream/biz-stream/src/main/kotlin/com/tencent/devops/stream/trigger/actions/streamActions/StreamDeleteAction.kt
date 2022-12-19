package com.tencent.devops.stream.trigger.actions.streamActions

import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.on.getTypesObjectKind
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.GitBaseAction
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState

/**
 * Stream 删除操作对应的action，因为可能同时设计tag和push的删除，所以这里选用代持gitAction来操作
 */
class StreamDeleteAction(
    private val gitAction: GitBaseAction
) : BaseAction {
    override val metaData: ActionMetaData = gitAction.metaData
    override var data: ActionData = gitAction.data
    override val api: StreamGitApiService = gitAction.api

    override fun init(): BaseAction? {
        gitAction.init()
        return this
    }

    override fun getProjectCode(gitProjectId: String?) = gitAction.getProjectCode(gitProjectId)
    override fun getGitProjectIdOrName(gitProjectId: String?) = gitAction.getGitProjectIdOrName(gitProjectId)

    override fun getGitCred(personToken: String?) = gitAction.getGitCred(personToken)

    override fun buildRequestEvent(eventStr: String) = gitAction.buildRequestEvent(eventStr)

    override fun skipStream() = gitAction.skipStream()

    override fun checkProjectConfig() = gitAction.checkProjectConfig()

    override fun checkMrConflict(
        path2PipelineExists: Map<String, StreamTriggerPipeline>
    ) = gitAction.checkMrConflict(path2PipelineExists)

    override fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>) {
        gitAction.checkAndDeletePipeline(path2PipelineExists)
    }

    override fun getYamlPathList(): List<YamlPathListEntry> {
        return GitActionCommon.getYamlPathList(
            action = gitAction,
            gitProjectId = getGitProjectIdOrName(),
            ref = data.context.defaultBranch
        ).map { (name, blobId) -> YamlPathListEntry(name, CheckType.NO_NEED_CHECK, data.context.defaultBranch, blobId) }
    }

    override fun getYamlContent(fileName: String): YamlContent {
        return YamlContent(
            ref = data.context.defaultBranch!!,
            content = api.getFileContent(
                cred = gitAction.getGitCred(),
                gitProjectId = getGitProjectIdOrName(),
                fileName = fileName,
                ref = data.context.defaultBranch!!,
                retry = ApiRequestRetryInfo(true)
            )
        )
    }

    override fun getChangeSet(): Set<String>? {
        return null
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        val deleteObjectKinds = triggerOn.delete?.getTypesObjectKind()?.map { it.value }?.toSet()
            ?: return TriggerResult(
                trigger = TriggerBody().triggerFail("on.delete.types", "does not currently exist"),
                timeTrigger = false,
                triggerOn = null,
                deleteTrigger = false
            )
        return if (gitAction.metaData.streamObjectKind.value in deleteObjectKinds) {
            TriggerResult(
                trigger = TriggerBody(true),
                timeTrigger = false,
                triggerOn = triggerOn,
                deleteTrigger = true
            )
        } else {
            TriggerResult(
                trigger = TriggerBody().triggerFail(
                    "on.delete.types",
                    "current type(${gitAction.metaData.streamObjectKind.value}) not match"
                ),
                timeTrigger = false,
                triggerOn = null,
                deleteTrigger = false
            )
        }
    }

    override fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>? = null

    override fun needSaveOrUpdateBranch(): Boolean = gitAction.needSaveOrUpdateBranch()

    override fun needSendCommitCheck() = gitAction.needSendCommitCheck()

    override fun needUpdateLastModifyUser(filePath: String) = false

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
        gitAction.sendCommitCheck(buildId, gitProjectName, state, block, context, targetUrl, description)
    }

    override fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook) {
        gitAction.registerCheckRepoTriggerCredentials(repoHook)
    }

    override fun updatePipelineLastBranchAndDisplayName(pipelineId: String, branch: String?, displayName: String?) {
        gitAction.updatePipelineLastBranchAndDisplayName(pipelineId, branch, displayName)
    }

    override fun getStartType() = gitAction.getStartType()
}
