package com.tencent.devops.stream.trigger.actions.streamActions

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_CREATE_TIME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_CREATOR
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.GitActionCommon
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.CheckType
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.util.CommonCredentialUtils
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory

@Suppress("ALL")
class StreamRepoTriggerAction(
    // 可能会包含stream action事件类似删除
    private val baseAction: BaseAction,
    private val client: Client,
    private val streamGitConfig: StreamGitConfig,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val redisOperation: RedisOperation,
    private val streamTriggerCache: StreamTriggerCache
) : BaseAction {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamRepoTriggerAction::class.java)
    }

    override val metaData: ActionMetaData = baseAction.metaData
    override var data: ActionData = baseAction.data
    override val api: StreamGitApiService = baseAction.api

    override fun init(): BaseAction? {
        baseAction.init()
        return this
    }

    override fun needAddWebhookParams() = true

    override fun getProjectCode(gitProjectId: String?) = baseAction.getProjectCode(gitProjectId)
    override fun getGitProjectIdOrName(gitProjectId: String?) = baseAction.getGitProjectIdOrName(gitProjectId)

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
        return GitActionCommon.getYamlPathList(
            action = baseAction,
            gitProjectId = getGitProjectIdOrName(),
            ref = data.context.repoTrigger!!.branch
        ).map { (name, blobId) ->
            YamlPathListEntry(name, CheckType.NO_NEED_CHECK, data.context.repoTrigger!!.branch, blobId)
        }
    }

    override fun getYamlContent(fileName: String): YamlContent {
        return YamlContent(
            ref = data.context.repoTrigger!!.branch,
            content = api.getFileContent(
                cred = baseAction.getGitCred(),
                gitProjectId = getGitProjectIdOrName(),
                fileName = fileName,
                ref = data.context.repoTrigger!!.branch,
                retry = ApiRequestRetryInfo(true)
            )
        )
    }

    override fun getChangeSet(): Set<String>? {
        return baseAction.getChangeSet()
    }

    override fun isMatch(triggerOn: TriggerOn): TriggerResult {
        triggerCheckRepoTriggerCredentials(triggerOn)
        check(triggerOn.repoHook).let {
            if (!it.trigger) return TriggerResult(
                trigger = it,
                triggerOn = triggerOn,
                timeTrigger = false,
                deleteTrigger = false
            )
        }
        return baseAction.isMatch(triggerOn)
    }

    override fun getUserVariables(
        yamlVariables: Map<String, Variable>?
    ): Map<String, Variable>? = baseAction.getUserVariables(yamlVariables)

    override fun needSaveOrUpdateBranch() = false

    override fun needSendCommitCheck() = baseAction.needSendCommitCheck()

    override fun needUpdateLastModifyUser(filePath: String) = false

    override fun checkIfModify() = baseAction.checkIfModify()

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

    override fun updatePipelineLastBranchAndDisplayName(pipelineId: String, branch: String?, displayName: String?) {}

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
            reasonParams = listOf("First level group[$firstGroupName] does not exist")
        )
    }

    override fun getStartType() = StartType.WEB_HOOK

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
                )
            )
        }
        val setting = streamBasicSettingService.getStreamConf(data.eventCommon.gitProjectId.toLong())
        if (setting == null && repoTriggerUserId != null) {
            RedisLock(
                redisOperation = redisOperation,
                lockKey = "REPO_HOOK_INIT_SETTING_${data.eventCommon.gitProjectId}",
                expiredTimeInSeconds = 5
            ).use {
                it.lock()
                // 锁后再读，避免并发线程重复去初始化导致报错
                if (streamBasicSettingService.getStreamConf(data.eventCommon.gitProjectId.toLong()) == null) {
                    streamBasicSettingService.initStreamConf(
                        userId = repoTriggerUserId,
                        projectId = GitCommonUtils.getCiProjectId(
                            data.eventCommon.gitProjectId,
                            streamGitConfig.getScmType()
                        ),
                        gitProjectId = data.eventCommon.gitProjectId.toLong(),
                        enabled = false
                    )
                }
            }
        }
        // 增加远程仓库时所使用权限的userId
        this.data.context.repoTrigger?.buildUserID = repoTriggerUserId
        logger.info(
            "StreamRepoTriggerActionafter|triggerCheckRepoTriggerCredentials" +
                "|check repoTrigger credentials|repoTrigger|${this.data.context.repoTrigger}"
        )
        streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = data.eventCommon.gitProjectId,
            action = this,
            getProjectInfo = api::getGitProjectInfo
        )?.let {
            data.context.repoTrigger?.triggerGitHttpUrl = it.gitHttpUrl
            data.context.repoCreatedTime = it.repoCreatedTime
            data.context.repoCreatorId = it.repoCreatorId
        }

        data.context.repoTrigger?.triggerGitHttpUrl = streamTriggerCache.getAndSaveRequestGitProjectInfo(
            gitProjectKey = data.eventCommon.gitProjectId,
            action = this,
            getProjectInfo = api::getGitProjectInfo
        )?.gitHttpUrl
        return repoTriggerUserId
    }

    private fun checkRepoTriggerCredentials(repoHook: RepositoryHook): Pair<Boolean, String?> {
        this.data.context.repoTrigger?.repoTriggerCred = when {
            repoHook.credentialsForTicketId != null -> {
                try {
                    val credential = CommonCredentialUtils.getCredential(
                        client = client,
                        projectId = GitCommonUtils.getCiProjectId(
                            this.data.getGitProjectId(),
                            streamGitConfig.getScmType()
                        ),
                        credentialId = repoHook.credentialsForTicketId!!,
                        typeCheck = listOf(CredentialType.ACCESSTOKEN, CredentialType.OAUTHTOKEN)
                    )
                    TGitCred(
                        userId = null,
                        accessToken = credential.v1,
                        useAccessToken = credential.credentialType == CredentialType.OAUTHTOKEN
                    )
                } catch (e: Throwable) {
                    throw StreamTriggerException(
                        action = this,
                        triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                        reasonParams = listOf("Credential [${repoHook.credentialsForTicketId}] does not exist")
                    )
                }
            }
            repoHook.credentialsForToken != null -> TGitCred(
                userId = null,
                accessToken = repoHook.credentialsForToken!!,
                useAccessToken = false
            )

            else -> throw StreamTriggerException(
                action = this,
                triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                reasonParams = listOf("credentials cannot be null")
            )
        }
        // stream 侧需要的是user 数字id 而不是 rtx
        val userInfo = try {
            this.api.getUserInfoByToken(
                this.data.context.repoTrigger?.repoTriggerCred as TGitCred
            ) ?: return Pair(false, null)
        } catch (e: Throwable) {
            throw StreamTriggerException(
                action = this,
                triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                reasonParams = listOf("401 Unauthorized. Repo:(${repoHook.name})")
            )
        }
        val check = try {
            this.api.getProjectUserInfo(
                cred = this.data.context.repoTrigger?.repoTriggerCred as TGitCred,
                userId = userInfo.id,
                gitProjectId = getGitProjectIdOrName(this.data.eventCommon.gitProjectId)
            ).accessLevel >= 40
        } catch (e: Throwable) {
            throw StreamTriggerException(
                action = this,
                triggerReason = TriggerReason.REPO_TRIGGER_FAILED,
                reasonParams = listOf("401 Unauthorized. Repo:(${repoHook.name}).user:(${userInfo.username})")
            )
        }
        return Pair(check, userInfo.username)
    }

    private fun check(repoHook: RepositoryHook?): TriggerBody {
        if (repoHook == null) {
            return TriggerBody()
        }

        repoHook.reposIgnore.forEach {
            if (this.data.eventCommon.gitProjectName == it.removeSuffix(GitCommonUtils.gitEnd)) {
                return TriggerBody().triggerFail("repo_hook.repos_ignore", "trigger repository($it) match")
            }
        }

        repoHook.reposIgnoreCondition.let {
            if (it.isEmpty()) return@let
            baseAction.parseStreamTriggerContext(this.data.context.repoTrigger?.repoTriggerCred)
            val supportVar = PipelineVarUtil.fillContextVarMap(
                mapOf(
                    PIPELINE_GIT_REPO_CREATE_TIME to (data.context.repoCreatedTime ?: ""),
                    PIPELINE_GIT_REPO_CREATOR to (data.context.repoCreatorId ?: "")
                )
            )
            it.forEach { condition ->
                // 进行表达式计算
                if (EnvReplacementParser.parse(
                        value = "\${{ $condition }}",
                        contextMap = supportVar,
                        onlyExpression = true
                    ).contains("false")
                ) {
                    return TriggerBody().triggerFail(
                        "repo_hook.repos_ignore_condition",
                        "trigger repository not match($condition), ci.repo_create_time" +
                            " is ${data.context.repoCreatedTime}, ci.repo_creator is ${data.context.repoCreatorId}"
                    )
                }
            }
        }
        return TriggerBody()
    }
}
