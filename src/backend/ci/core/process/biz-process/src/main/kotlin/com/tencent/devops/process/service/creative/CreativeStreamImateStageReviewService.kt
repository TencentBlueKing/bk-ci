package com.tencent.devops.process.service.creative

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_IMATE_SESSION_ID
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_IMATE_STAGE_REVIEW_DENIED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_IMATE_STAGE_REVIEW_PASSED
import com.tencent.devops.process.engine.pojo.PipelineBuildStage
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.cds.CdsOpenApprovalClient
import com.tencent.devops.process.pojo.creative.CreativeStreamStageReviewContent
import com.tencent.devops.process.pojo.creative.CreativeStreamStageReviewHint
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.utils.ImateLockDecision
import com.tencent.devops.process.utils.ImateStageReview
import com.tencent.devops.process.utils.PIPELINE_NAME
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CreativeStreamImateStageReviewService(
    private val cdsOpenApprovalClient: CdsOpenApprovalClient,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildVariableService: BuildVariableService,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineUrlBean: PipelineUrlBean
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CreativeStreamImateStageReviewService::class.java)
        private val ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    fun hint(projectId: String, pipelineId: String, buildId: String): CreativeStreamStageReviewHint? {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId) ?: return null
        if (buildInfo.pipelineId != pipelineId) return null
        if (buildInfo.channelCode != ChannelCode.CREATIVE_STREAM) {
            return null
        }
        val pending = pipelineStageService.getPendingStage(projectId, buildId) ?: return null
        if (pending.status != BuildStatus.PAUSE) return null
        val group = pending.checkIn?.groupToReview() ?: return null
        val variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        val session = variables[CI_IMATE_SESSION_ID]
        val lockRequired = ImateStageReview.needLock(buildInfo.channelCode, group.name, session)
        return CreativeStreamStageReviewHint(
            stageId = pending.stageId,
            groupId = group.id,
            groupName = group.name,
            imateLockRequired = lockRequired,
            taskId = if (lockRequired) {
                ImateStageReview.taskId(projectId, buildId, pending.stageId, pending.executeCount)
            } else null,
            saasId = if (lockRequired) ImateStageReview.SAAS_ID else null
        )
    }

    fun assertLock(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        isCancel: Boolean,
        buildStage: PipelineBuildStage,
        channelCode: ChannelCode
    ) {
        val group = buildStage.checkIn?.groupToReview()
        // 先用内存字段短路：普通流水线、非 IMATE 组零额外 IO
        if (!ImateStageReview.isImateGroup(group?.name)) return
        if (channelCode != ChannelCode.CREATIVE_STREAM) return
        val boundSession = buildVariableService.getAllVariable(projectId, pipelineId, buildId)[CI_IMATE_SESSION_ID]
        if (!ImateStageReview.needLock(channelCode, group?.name, boundSession)) return
        val taskId = ImateStageReview.taskId(projectId, buildId, stageId, buildStage.executeCount)
        val query = cdsOpenApprovalClient.getDetail(taskId)
        val decision = ImateStageReview.decideLock(
            configured = query.configured,
            queryFailed = query.failed,
            notFound = query.notFound,
            isCancel = isCancel,
            cdsStatus = query.detail?.status,
            cdsSessionKey = query.detail?.sessionKey,
            boundSessionId = boundSession
        )
        logger.info(
            "[$buildId]|IMATE_STAGE_REVIEW|user=$userId|taskId=$taskId|cancel=$isCancel|" +
                "decision=$decision|cdsStatus=${query.detail?.status}|" +
                "conversationId=${ImateStageReview.conversationIdOf(ImateStageReview.toAgent2UserKey(boundSession))}"
        )
        if (decision == ImateLockDecision.PASS) {
            logYellow(
                buildStage,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_IMATE_STAGE_REVIEW_PASSED,
                    params = arrayOf(taskId, query.detail?.approver.orEmpty())
                )
            )
            return
        }
        val errorCode = when (decision) {
            ImateLockDecision.REJECTED -> ProcessMessageCode.ERROR_IMATE_STAGE_REVIEW_REJECTED
            ImateLockDecision.SESSION_MISMATCH -> ProcessMessageCode.ERROR_IMATE_STAGE_REVIEW_SESSION_MISMATCH
            ImateLockDecision.QUERY_FAILED -> ProcessMessageCode.ERROR_IMATE_STAGE_REVIEW_QUERY_FAILED
            ImateLockDecision.NOT_CONFIGURED -> ProcessMessageCode.ERROR_IMATE_STAGE_REVIEW_NOT_CONFIGURED
            else -> ProcessMessageCode.ERROR_IMATE_STAGE_REVIEW_NOT_LOCKED
        }
        val message = I18nUtil.getCodeLanMessage(
            messageCode = errorCode,
            params = arrayOf(taskId)
        )
        logRed(
            buildStage,
            I18nUtil.getCodeLanMessage(
                messageCode = BK_IMATE_STAGE_REVIEW_DENIED,
                params = arrayOf(message)
            )
        )
        throw ErrorCodeException(
            statusCode = Response.Status.FORBIDDEN.statusCode,
            errorCode = errorCode,
            params = arrayOf(taskId)
        )
    }

    fun getContent(taskId: String): CreativeStreamStageReviewContent {
        val parts = ImateStageReview.parseTaskId(taskId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_IMATE_STAGE_REVIEW_TASK_INVALID,
                params = arrayOf(taskId)
            )
        val buildInfo = pipelineRuntimeService.getBuildInfo(parts.projectId, parts.buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(parts.buildId)
            )
        val stage = pipelineStageService.getStage(parts.projectId, parts.buildId, parts.stageId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_STAGE_EXISTS_BY_ID,
                params = arrayOf(parts.stageId)
            )
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(parts.projectId, buildInfo.pipelineId)
        val pipelineName = pipelineInfo?.pipelineName ?: buildVariableService.getAllVariable(
            parts.projectId, buildInfo.pipelineId, parts.buildId
        )[PIPELINE_NAME] ?: buildInfo.pipelineId
        val group = stage.checkIn?.groupToReview()
        val reviewers = group?.reviewers?.filter { it.isNotBlank() }?.joinToString("、").orEmpty()
        val detailUrl = pipelineUrlBean.genBuildDetailUrl(
            projectCode = parts.projectId,
            pipelineId = buildInfo.pipelineId,
            buildId = parts.buildId,
            position = null,
            stageId = parts.stageId,
            needShortUrl = false,
            channelCode = buildInfo.channelCode
        )
        val timeoutHours = (stage.checkIn?.timeout ?: 24).coerceAtLeast(1)
        val expireAt = (stage.startTime ?: LocalDateTime.now()).plusHours(timeoutHours.toLong())
        val stageLabel = stage.name?.takeIf { it.isNotBlank() } ?: parts.stageId
        val title = "创作流审核：$pipelineName #${buildInfo.buildNum} / $stageLabel"
        val content = buildString {
            appendLine("## 创作流 Stage 审核")
            appendLine()
            appendLine("- 创作流：**$pipelineName**")
            appendLine("- 构建：#${buildInfo.buildNum}")
            appendLine("- 阶段：$stageLabel (`${parts.stageId}`)")
            appendLine("- 审核组：${group?.name ?: ImateStageReview.GROUP_NAME}")
            appendLine("- 审核说明：${stage.checkIn?.reviewDesc?.takeIf { it.isNotBlank() } ?: "-"}")
            appendLine("- 审核人：$reviewers")
            appendLine("- 详情：[$detailUrl]($detailUrl)")
            appendLine()
            appendLine("请在 **imate** 会话中点击锁定完成审核。通过后 Agent 将继续执行本 Stage；驳回则终止。")
        }
        return CreativeStreamStageReviewContent(
            title = title,
            approvalContent = content.trim(),
            expireAt = expireAt.format(ISO_LOCAL)
        )
    }

    private fun logYellow(stage: PipelineBuildStage, message: String) {
        buildLogPrinter.addYellowLine(
            buildId = stage.buildId,
            message = message,
            tag = stage.stageId,
            executeCount = stage.executeCount,
            jobId = null,
            stepId = null,
            projectId = stage.projectId,
            pipelineId = stage.pipelineId
        )
    }

    private fun logRed(stage: PipelineBuildStage, message: String) {
        buildLogPrinter.addRedLine(
            buildId = stage.buildId,
            message = message,
            tag = stage.stageId,
            containerHashId = null,
            executeCount = stage.executeCount,
            jobId = null,
            stepId = null,
            projectId = stage.projectId,
            pipelineId = stage.pipelineId
        )
    }
}
