package com.tencent.devops.process.plugin.check.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_ACTION
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.RetryUtils
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
import com.tencent.devops.model.process.tables.records.TPipelineBuildCheckRunRecord
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.process.plugin.check.dao.PipelineBuildCheckRunDao
import com.tencent.devops.process.pojo.CheckRunExtension
import com.tencent.devops.process.pojo.PipelineBuildCheckRun
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.repository.api.ServiceRepositoryConfigResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.enums.CheckRunConclusion
import com.tencent.devops.scm.api.enums.CheckRunStatus
import com.tencent.devops.scm.api.enums.ScmProviderCodes
import com.tencent.devops.scm.api.pojo.CheckRun
import com.tencent.devops.scm.api.pojo.CheckRunInput
import com.tencent.devops.scm.api.pojo.CheckRunOutput
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@SuppressWarnings("ALL")
class PipelineCheckRunService @Autowired constructor(
    val pipelineBuildCheckRunDao: PipelineBuildCheckRunDao,
    val pipelineBuildFacadeService: PipelineBuildFacadeService,
    val pipelineCheckRunQualityService: PipelineCheckRunQualityService,
    val redisOperation: RedisOperation,
    val client: Client,
    val dslContext: DSLContext
) {
    @Scheduled(initialDelay = 20_000, fixedDelay = 120_000)
    fun reHandleCheckRun() {
        val checkRunRecords = pipelineBuildCheckRunDao.getCheckRun(
            dslContext = dslContext,
            checkRunStatus = CheckRunStatus.IN_PROGRESS.name,
            buildStatus = setOf(BuildStatus.FAILED.name, BuildStatus.SUCCEED.name)
        )
        checkRunRecords.forEach { checkRun ->
            val buildId = checkRun.buildId
            val lockKey = "code_fix_check_run_$buildId"
            val checkRunKey = "${checkRun.repoHashId}|${checkRun.commitId}|" +
                    "${checkRun.pullRequestId}|${checkRun.context}"
            val redisLock = RedisLock(redisOperation, lockKey, 60)
            logger.info("trying to fix abnormal check-run($checkRunKey) for build($buildId)")
            if (redisLock.tryLock()) {
                RetryUtils.execute(
                    object : RetryUtils.Action<Unit> {
                        override fun fail(e: Throwable) {
                            logger.warn(
                                "BKSystemMonitor|fix check-run fail for build($buildId)|e=${e.message}", e
                            )
                            // 多次操作失败，记录失败
                            pipelineBuildCheckRunDao.update(
                                dslContext = dslContext,
                                projectId = checkRun.projectId,
                                pipelineId = checkRun.pipelineId,
                                buildId = buildId,
                                checkRunId = null,
                                checkRunStatus = CHECK_RUN_FIX_FAILED
                            )
                        }

                        override fun execute() {
                            val extensionData = parseExtensionData(
                                extensionDataStr = checkRun.extensionData,
                                buildId = buildId
                            ) ?: run {
                                pipelineBuildCheckRunDao.update(
                                    dslContext = dslContext,
                                    projectId = checkRun.projectId,
                                    pipelineId = checkRun.pipelineId,
                                    buildId = buildId,
                                    checkRunId = null,
                                    checkRunStatus = CHECK_RUN_SKIP_FLAG
                                )
                                return
                            }
                            createCheckRun(
                                projectId = checkRun.projectId,
                                repositoryConfig = RepositoryConfig(
                                    repositoryType = RepositoryType.ID,
                                    repositoryName = null,
                                    repositoryHashId = checkRun.repoHashId
                                ),
                                checkRunInput = extensionData.checkRunInput
                            )
                            logger.info(
                                "fixed the abnormal check-run($checkRunKey) successfully for build($buildId)"
                            )
                            pipelineBuildCheckRunDao.update(
                                dslContext = dslContext,
                                projectId = checkRun.projectId,
                                pipelineId = checkRun.pipelineId,
                                buildId = buildId,
                                checkRunId = null,
                                checkRunStatus = extensionData.checkRunInput.status.name
                            )
                        }
                    },
                    3,
                    SLEEP_SECOND_FOR_RETRY_10 * 1000
                )
            } else {
                logger.info(
                    "[$lockKey]|PIPELINE_FIX_CHECK_RUN|lock fail, skip!"
                )
            }
        }
    }

    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) = with(event) {
        resolveCheckRun(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            triggerType = triggerType,
            buildStatus = BuildStatus.RUNNING
        ) { handleCheckRun(it) }
    }


    fun onBuildFinished(event: PipelineBuildFinishBroadCastEvent) = with(event) {
        resolveCheckRun(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            triggerType = triggerType,
            buildStatus = BuildStatus.valueOf(status)
        ) { handleCheckRun(it) }
    }

    /**
     * 提取check-run相关核心信息
     */
    private fun resolveCheckRun(
        projectId: String,
        pipelineId: String,
        buildId: String,
        triggerType: String,
        buildStatus: BuildStatus,
        action: (PipelineBuildCheckRun) -> Unit
    ) {
        if (triggerType != StartType.WEB_HOOK.name) {
            logger.info("skip resolve check run|process instance($buildId) is not web hook triggered")
            return
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
            return
        }
        val variables = buildHistoryVar.variables
        if (variables.isEmpty()) {
            logger.warn("skip resolve check run|process instance($buildId) variables is empty")
            return
        }
        val enableCheck = variables[BK_REPO_GIT_WEBHOOK_ENABLE_CHECK]?.toBoolean() ?: true
        if (!enableCheck) {
            logger.warn("skip resolve check run|process instance($buildId) has check-run disabled")
        }
        // 渠道校验
        val channelCode = variables[PIPELINE_START_CHANNEL]?.let { ChannelCode.getChannel(it) }
        if (!supportChannel(channelCode)) {
            logger.warn("skip resolve check run|process instance($buildId) is not bs or gongfengscan channel")
            return
        }
        // 事件校验
        val webhookType = CodeType.convert(variables[PIPELINE_WEBHOOK_TYPE])
        val webhookEventType = CodeEventType.convert(variables[PIPELINE_WEBHOOK_EVENT_TYPE])
        if (!supportCodeType(webhookType) || !supportEventType(webhookEventType)) {
            logger.info(
                "skip resolve check run|process instance($buildId) not support write check run|" +
                        "hookType[$webhookType]|eventType[$webhookEventType]"
            )
            return
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
            return
        }

        val repositoryConfig = when (repositoryType) {
            RepositoryType.ID -> RepositoryConfig(repositoryId, null, repositoryType)
            RepositoryType.NAME -> RepositoryConfig(null, repositoryId, repositoryType)
        }
        // 获取仓库信息
        val repo = getRepository(projectId, repositoryConfig) ?: run {
            logger.warn("skip resolve check run|process instance($buildId) trigger repository not exist")
            return
        }
        val scmConfig = getScmConfig(repo.scmCode)!!
        val providerCode = ScmProviderCodes.valueOf(scmConfig.providerCode)
        if (!supportRepoProvider(providerCode)) {
            logger.warn("skip resolve check run|process instance($buildId) not support write $providerCode check run")
            return
        }
        val block = variables[PIPELINE_WEBHOOK_BLOCK]?.toBoolean() ?: false
        val mrId = variables[PIPELINE_WEBHOOK_MR_ID]?.toLong() ?: run {
            if (finalEventType.isMergeRequest()) {
                logger.warn("skip resolve check run|process instance($buildId) mrId is empty")
                return
            }
            0L
        }
        val userId = variables[PIPELINE_START_USER_ID]
        val finalBuildStatus = transferBuildStatus(buildStatus)
        action(
            PipelineBuildCheckRun(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                buildNum = buildHistoryVar.buildNum,
                buildStatus = finalBuildStatus,
                repoHashId = repo.repoHashId!!,
                targetBranches = getTargetBranches(finalEventType, variables),
                commitId = commitId,
                context = context,
                block = block,
                pullRequestId = mrId,
                buildVariables = variables,
                eventType = finalEventType,
                pipelineName = pipelineName,
                repoType = webhookType!!,
                triggerType = triggerType,
                channelCode = channelCode!!,
                repositoryConfig = repositoryConfig,
                lockKey = getLockKey(repo, pipelineId),
                externalId = "${userId}_${projectId}_${pipelineId}_$buildId",
                startTime = if (buildHistoryVar.startTime <= 0) {
                    LocalDateTime.now().timestamp()
                } else {
                    buildHistoryVar.startTime
                },
                checkRunStatus = getCheckRunState(finalBuildStatus).first
            )
        )
    }

    private fun handleCheckRun(checkRun: PipelineBuildCheckRun) {
        with(checkRun) {
            while (true) {
                val redisLock = RedisLock(redisOperation, lockKey, 60)
                redisLock.use {
                    if (!redisLock.tryLock()) {
                        logger.info("Code web hook check run try lock($lockKey) fail")
                        Thread.sleep(100)
                        return@use
                    }
                    // 业务PrId
                    val pullRequestBizId = getPullRequestBizId()
                    // 满足当前版本的构建记录
                    val checkRunRecords = pipelineBuildCheckRunDao.list(
                        dslContext = dslContext,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        repoHashId = repoHashId,
                        commitId = commitId,
                        pullRequestId = pullRequestBizId
                    )
                    // 最新的构建任务
                    val checkRunRecord = checkRunRecords.firstOrNull()
                    val checkRunInput = convert()
                    // 需要跳过的记录
                    var skipRecords = mutableListOf<TPipelineBuildCheckRunRecord>()
                    val now = LocalDateTime.now()
                    when {
                        // [源分支推送更新] 或 [事件回放触发新的构建任务]
                        checkRunRecord == null || buildNum > checkRunRecord.buildNum -> {
                            logger.info(
                                "[$buildId]attempting to add check-run(${key()})"
                            )
                            pipelineBuildCheckRunDao.create(
                                dslContext = dslContext,
                                buildCheckRun = this.copy(pullRequestBizId = pullRequestBizId)
                            )
                            addCheckRun(
                                projectId = projectId,
                                repositoryConfig = repositoryConfig,
                                checkRunInput = checkRunInput
                            )?.let {
                                pipelineBuildCheckRunDao.update(
                                    dslContext = dslContext,
                                    projectId = projectId,
                                    pipelineId = pipelineId,
                                    buildId = buildId,
                                    checkRunId = it.id
                                )
                            }
                        }

                        buildNum == checkRunRecord.buildNum -> {
                            // 重试场景
                            val checkRunInfo = if (buildStatus == BuildStatus.RUNNING) {
                                logger.info("overwriting existing check-run task with updated information")
                                // 更新开始时间/check-run状态
                                checkRunRecord.setCreateTime(now).setCheckRunStatus(CheckRunStatus.IN_PROGRESS.name)
                                addCheckRun(
                                    projectId = projectId,
                                    repositoryConfig = repositoryConfig,
                                    checkRunInput = checkRunInput
                                )
                            } else {
                                val checkRunId = checkRunRecord.checkRunId ?: run {
                                    logger.warn(
                                        "process instance($buildId) checkRunId is empty, write failure possible"
                                    )
                                    null
                                }
                                checkRunId?.let {
                                    updateCheckRun(
                                        projectId = projectId,
                                        repositoryConfig = repositoryConfig,
                                        checkRunInput = checkRunInput.copy(id = it)
                                    )
                                }
                            }
                            logger.info(
                                "[$buildId]attempting to update check-run(${checkRunInfo?.id}) to " +
                                        "repo($repoHashId)|ref($commitId)|extRef($pullRequestBizId)|context($context)"
                            )
                            checkRunInfo?.let {
                                checkRunRecord.setCheckRunStatus(checkRunInput.status.name)
                            }
                            pipelineBuildCheckRunDao.update(
                                dslContext = dslContext,
                                record = checkRunRecord.setCheckRunId(checkRunInfo?.id ?: 0L)
                                        .setBuildNum(buildNum)
                                        .setUpdateTime(now)
                                        .setBuildStatus(buildStatus.name)
                                        .setExtensionData(
                                            // 保存扩展信息，后续修复异常数据时，直接使用
                                            JsonUtil.toJson(
                                                CheckRunExtension(
                                                    checkRunInput = checkRunInput
                                                ),
                                                false
                                            )
                                        )
                            )
                            if (checkRunRecords.size > 1) {
                                skipRecords = checkRunRecords.subList(1, checkRunRecords.size)
                            }
                        }

                        else -> {
                            logger.info(
                                "skip resolve check run|process instance($buildId) is not the latest one; " +
                                        "the latest build instance is (${checkRunRecord.buildId}#" +
                                        "${checkRunRecord.buildNum})"
                            )
                            skipRecords.addAll(checkRunRecords)
                        }
                    }
                    skipCheckRunRecord(skipRecords)
                    return
                }
            }
        }
    }

    /**
     * 修改无效数据的构建状态
     */
    fun skipCheckRunRecord(checkRunRecords: List<TPipelineBuildCheckRunRecord>) {
        if (checkRunRecords.isEmpty()) {
            return
        }
        logger.info("Invalid data exists; try to modify the build status")
        checkRunRecords.forEach {
            it.checkRunStatus = CHECK_RUN_SKIP_FLAG
            pipelineBuildCheckRunDao.update(
                dslContext = dslContext,
                record = it
            )
        }
    }

    private fun PipelineBuildCheckRun.convert(): CheckRunInput {
        val (checkRunState, checkRunConclusion) = getCheckRunState(buildStatus)
        val buildUrl = getBuildUrl(projectId, pipelineId, buildId, channelCode, buildVariables)
        // 执行结束后尝试获取报表
        val quality = if (buildStatus != BuildStatus.RUNNING && supportQuality(eventType)) {
            pipelineCheckRunQualityService.getQuality(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variables = buildVariables,
                startTime = startTime ?: 0L,
                eventStatus = buildStatus.name,
                triggerType = triggerType,
                channelCode = channelCode,
                detailUrl = buildUrl,
                pipelineName = pipelineName
            )
        } else {
            null
        }
        return CheckRunInput(
            name = context,
            ref = commitId,
            status = checkRunState,
            conclusion = checkRunConclusion,
            pullRequestId = pullRequestId,
            targetBranches = targetBranches,
            startedAt = startTime?.let {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            } ?: LocalDateTime.now(),
            block = block,
            output = CheckRunOutput(
                title = pipelineName,
                summary = getSummary(pipelineName, buildStatus),
                text = quality
            ),
            completedAt = if (buildStatus != BuildStatus.RUNNING) {
                LocalDateTime.now()
            } else {
                null
            },
            detailsUrl = buildUrl,
            externalId = externalId
        )
    }

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

    private fun getScmConfig(scmCode: String) = try {
        client.get(ServiceRepositoryConfigResource::class).getSummary(scmCode).data
    } catch (ignored: Exception) {
        logger.warn("Failed to get SCM config for scmCode: {scmCode}", ignored)
        null
    }

    /**
     * 获取check-run状态
     */
    private fun getCheckRunState(buildStatus: BuildStatus) = when (buildStatus) {
        BuildStatus.RUNNING -> CheckRunStatus.IN_PROGRESS to null
        BuildStatus.SUCCEED -> CheckRunStatus.COMPLETED to CheckRunConclusion.SUCCESS
        else -> CheckRunStatus.COMPLETED to CheckRunConclusion.FAILURE
    }

    /**
     * 支持的仓库
     */
    private fun supportCodeType(codeType: CodeType?) = listOf(
        CodeType.GIT,
        CodeType.TGIT,
        CodeType.GITHUB
    ).contains(codeType)

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

    private fun supportQuality(eventType: CodeEventType?) = listOf(
        CodeEventType.PULL_REQUEST,
        CodeEventType.MERGE_REQUEST,
        CodeEventType.MERGE_REQUEST_ACCEPT
    ).contains(eventType)

    /**
     * 支持的仓库，目前仅开放GITEE
     */
    private fun supportRepoProvider(scmProviderCodes: ScmProviderCodes) = listOf(
        ScmProviderCodes.GITEE
    ).contains(scmProviderCodes)

    private fun getSummary(pipelineName: String, buildStatus: BuildStatus) = when (buildStatus) {
        BuildStatus.RUNNING -> "Your pipeline [$pipelineName] is running"
        BuildStatus.SUCCEED -> "Your pipeline [$pipelineName] is succeed"
        else -> "Your pipeline [$pipelineName] is failed"
    }

    private fun getBuildUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        variables: Map<String, String>
    ) = if (channelCode == ChannelCode.CODECC) {
        val codeccTaskId = variables[CodeccUtils.BK_CI_CODECC_TASK_ID]
        val codeccPrefix = "${HomeHostUtil.innerCodeccHost()}/codecc/$projectId/task"
        if (codeccTaskId != null) {
            "$codeccPrefix/$codeccTaskId/detail"
        } else {
            "codeccPrefix/list?pipelineId=$pipelineId&buildId=$buildId&from=check_run"
        }
    } else {
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/$pipelineId/detail/$buildId"
    }

    /**
     * 获取业务PrId
     * PUSH -> ~NONE
     * MR -> MR_ID
     */
    private fun PipelineBuildCheckRun.getPullRequestBizId() = when {
        eventType == CodeEventType.PUSH -> PUSH_EVENT_CHECK_RUN_FLAG
        eventType.isMergeRequest() -> pullRequestId.toString()
        else -> ""
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

    private fun getLockKey(repository: Repository, pipelineId: String) = when (repository) {
        is CodeGitRepository, is CodeTGitRepository -> {
            "code_git_commit_check_lock_$pipelineId"
        }

        is GithubRepository -> {
            "code_github_check_run_lock_$pipelineId"
        }

        else -> {
            "lock:pipeline:$pipelineId:build:check:run"
        }
    }

    private fun addCheckRun(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        checkRunInput: CheckRunInput
    ): CheckRun? {
        return try {
            createCheckRun(
                projectId = projectId,
                repositoryConfig = repositoryConfig,
                checkRunInput = checkRunInput
            )
        } catch (ignored: Exception) {
            logger.warn("failed to add check-run", ignored)
            null
        }
    }

    fun createCheckRun(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        checkRunInput: CheckRunInput
    ) = client.get(ServiceScmRepositoryApiResource::class).createCheckRun(
        projectId,
        repoType = repositoryConfig.repositoryType,
        repoId = repositoryConfig.getRepositoryId(),
        checkRunInput = checkRunInput
    ).data

    private fun updateCheckRun(
        projectId: String,
        repositoryConfig: RepositoryConfig,
        checkRunInput: CheckRunInput
    ): CheckRun? {
        return try {
            client.get(ServiceScmRepositoryApiResource::class).updateCheckRun(
                projectId,
                repoType = repositoryConfig.repositoryType,
                repoId = repositoryConfig.getRepositoryId(),
                checkRunInput = checkRunInput
            ).data
        } catch (ignored: Exception) {
            logger.warn("failed to update check-run", ignored)
            null
        }
    }

    private fun PipelineBuildCheckRun.key() =
        "repo($repoHashId)|ref($commitId)|extRef($extensionData)|context($context)"

    private fun transferBuildStatus(buildStatus: BuildStatus) = when (buildStatus) {
        BuildStatus.RUNNING, BuildStatus.SUCCEED -> buildStatus
        else -> BuildStatus.FAILED
    }

    private fun parseExtensionData(extensionDataStr: String?, buildId: String): CheckRunExtension? {
        return if (extensionDataStr.isNullOrBlank()) {
            logger.info(
                "skip fix check run|process instance($buildId) extensionData is empty"
            )
            null
        } else {
            try {
                JsonUtil.to(extensionDataStr, CheckRunExtension::class.java)
            } catch (e: Exception) {
                logger.warn(
                    "skip fix check run|failed to parse extension data[$extensionDataStr] for" +
                            "process instance($buildId)"
                )
                null
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCheckRunService::class.java)
        private const val PUSH_EVENT_CHECK_RUN_FLAG = "~NONE"
        private const val CHECK_RUN_FIX_FAILED = "CHECK_RUN_FIX_FAILED"
        private const val CHECK_RUN_SKIP_FLAG = "SKIP"
        private const val SLEEP_SECOND_FOR_RETRY_10: Long = 10
    }
}