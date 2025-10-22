package com.tencent.devops.process.trigger.check

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ACTION
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.webhook.enums.code.tgit.TGitMergeActionKind
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_ENABLE_CHECK
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BLOCK
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REPO_TYPE
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TYPE
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.trigger.pojo.PipelineBuildCheckRunContext
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineBuildCheckRunResolver @Autowired constructor(
    private val client: Client,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) {

    /**
     * 提取check-run相关核心信息
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun resolveVar(
        projectId: String,
        pipelineId: String,
        buildId: String,
        triggerType: String,
        buildStatus: BuildStatus
    ): PipelineBuildCheckRunContext? {
        if (triggerType != StartType.WEB_HOOK.name) {
            logger.info("skip resolve check run|process instance($buildId) is not web hook triggered")
            return null
        }
        // 构建信息
        val buildHistoryVar = pipelineBuildFacadeService.getBuildVars(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            userId = "",
            checkPermission = false
        ).data ?: run {
            logger.warn("skip resolve check run|process instance($buildId) not exist")
            return null
        }
        val variables = buildHistoryVar.variables
        if (variables.isEmpty()) {
            logger.warn("skip resolve check run|process instance($buildId) variables is empty")
            return null
        }
        val enableCheck = variables[BK_REPO_GIT_WEBHOOK_ENABLE_CHECK]?.toBoolean() ?: true
        if (!enableCheck) {
            logger.warn("skip resolve check run|process instance($buildId) has check-run disabled")
        }
        // 渠道校验
        val channelCode = variables[PIPELINE_START_CHANNEL]?.let { ChannelCode.getChannel(it) }
        if (!supportChannel(channelCode)) {
            logger.warn("skip resolve check run|process instance($buildId) is not bs or gongfengscan channel")
            return null
        }
        // 事件校验
        val webhookType = CodeType.convert(variables[PIPELINE_WEBHOOK_TYPE])
        val webhookEventType = CodeEventType.convert(variables[PIPELINE_WEBHOOK_EVENT_TYPE])
        if (!supportCodeType(webhookType) || !supportEventType(webhookEventType)) {
            logger.info(
                "skip resolve check run|process instance($buildId) not support write check run|" +
                        "hookType[$webhookType]|eventType[$webhookEventType]"
            )
            return null
        }
        val webhookMrEventAction = variables[PIPELINE_GIT_MR_ACTION]
        // GIT触发器v2, MR合并操作时将webhookEventType置为 [MERGE_REQUEST_ACCEPT]
        val finalEventType = if (webhookEventType == CodeEventType.MERGE_REQUEST &&
            webhookMrEventAction == TGitMergeActionKind.MERGE.value
        ) {
            CodeEventType.MERGE_REQUEST_ACCEPT
        } else {
            webhookEventType
        }
        val pipelineName = buildHistoryVar.pipelineName
        val context = "$pipelineName@${finalEventType!!.name}"
        val commitId = variables[PIPELINE_WEBHOOK_REVISION]
        var repositoryId = variables[PIPELINE_WEBHOOK_REPO]
        if (repositoryId.isNullOrBlank()) {
            // 兼容老的V1的
            repositoryId = variables["hookRepo"]
        }
        val repositoryType = RepositoryType.valueOf(
            variables[PIPELINE_WEBHOOK_REPO_TYPE] ?: RepositoryType.ID.name
        )
        // 核心参数校验
        if (commitId.isNullOrEmpty() || repositoryId.isNullOrEmpty()) {
            logger.warn(
                "skip resolve check run|process instance($buildId) commitId($commitId) repoHashId($repositoryId) " +
                        "repositoryType($repositoryType) is empty"
            )
            return null
        }

        val repositoryConfig = when (repositoryType) {
            RepositoryType.ID -> RepositoryConfig(repositoryId, null, repositoryType)
            RepositoryType.NAME -> RepositoryConfig(null, repositoryId, repositoryType)
        }
        // 获取仓库信息
        val repo = getRepository(projectId, repositoryConfig) ?: run {
            logger.warn("skip resolve check run|process instance($buildId) trigger repository not exist")
            return null
        }
        val block = variables[PIPELINE_WEBHOOK_BLOCK]?.toBoolean() ?: false
        val mrId = variables[PIPELINE_WEBHOOK_MR_ID]?.toLong() ?: run {
            if (finalEventType.isMergeRequest()) {
                logger.warn("skip resolve check run|process instance($buildId) mrId is empty")
                return null
            }
            0L
        }
        val userId = variables[PIPELINE_START_USER_ID]
        val finalBuildStatus = transferBuildStatus(buildStatus)
        return PipelineBuildCheckRunContext(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            buildNum = buildHistoryVar.buildNum,
            buildStatus = finalBuildStatus,
            buildVariables = variables,
            startTime = if (buildHistoryVar.startTime <= 0) {
                LocalDateTime.now().timestamp()
            } else {
                buildHistoryVar.startTime
            },
            pipelineName = pipelineName,
            triggerType = triggerType,
            channelCode = channelCode!!,
            buildUrl = getBuildUrl(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = channelCode,
                variables = variables
            ),
            repoHashId = repo.repoHashId!!,
            eventType = finalEventType,
            repoType = webhookType!!,
            commitId = commitId,
            context = context,
            block = block,
            pullRequestId = mrId,
            targetBranches = getTargetBranches(finalEventType, variables),
            externalId = "${userId}_${projectId}_${pipelineId}_$buildId"
        )
    }

    /**
     * 支持的事件类型
     */
    private fun supportEventType(eventType: CodeEventType?) = listOf(
        CodeEventType.PULL_REQUEST,
        CodeEventType.MERGE_REQUEST,
        CodeEventType.MERGE_REQUEST_ACCEPT,
        CodeEventType.PUSH
    ).contains(eventType)

    private fun supportChannel(channelCode: ChannelCode?) = listOf(
        ChannelCode.BS,
        ChannelCode.GONGFENGSCAN
    ).contains(channelCode)

    /**
     * 支持的仓库
     */
    private fun supportCodeType(codeType: CodeType?) = listOf(
        CodeType.SCM_GIT
    ).contains(codeType)

    private fun getRepository(projectId: String, repositoryConfig: RepositoryConfig) = try {
        client.get(ServiceRepositoryResource::class).get(
            projectId,
            repositoryConfig.getRepositoryId(),
            repositoryConfig.repositoryType
        ).data
    } catch (ignored: Exception) {
        logger.warn("get $projectId repository($repositoryConfig) fail", ignored)
        null
    }

    private fun transferBuildStatus(buildStatus: BuildStatus) = when (buildStatus) {
        BuildStatus.RUNNING, BuildStatus.SUCCEED -> buildStatus
        else -> BuildStatus.FAILED
    }

    /**
     * 获取目标分支
     * PUSH -> ~NONE
     * MR -> 目标分支
     */
    private fun getTargetBranches(eventType: CodeEventType, variables: Map<String, String>) = when {
        eventType == CodeEventType.PUSH -> listOf(PUSH_EVENT_CHECK_RUN_FLAG)
        eventType.isMergeRequest() -> listOf(variables[PIPELINE_WEBHOOK_TARGET_BRANCH] ?: "")
        else -> listOf()
    }

    private fun getBuildUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        variables: Map<String, String>
    ) = if (channelCode == ChannelCode.GONGFENGSCAN) {
        val codeccTaskId = variables[CodeccUtils.BK_CI_CODECC_TASK_ID]
        val codeccPrefix = "${HomeHostUtil.innerCodeccHost()}/codecc/$projectId/task"
        if (codeccTaskId != null) {
            "$codeccPrefix/$codeccTaskId/detail"
        } else {
            "$codeccPrefix/list?pipelineId=$pipelineId&buildId=$buildId&from=check_run"
        }
    } else {
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
    }

    companion object {
        private const val PUSH_EVENT_CHECK_RUN_FLAG = "~NONE"
        private val logger = LoggerFactory.getLogger(PipelineBuildCheckRunResolver::class.java)
    }
}
