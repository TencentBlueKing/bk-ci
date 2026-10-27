package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.DependOnJob
import com.tencent.devops.process.utils.DependOnUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage 首次启动时用运行时变量重算 Job dependOn，并校验循环依赖
 */
@Service
class DependOnStageCmd(
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildLogPrinter: BuildLogPrinter
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.buildStatus.isReadyToRun()
    }

    override fun execute(commandContext: StageContext) {
        val stageContainers = commandContext.containers.filter { container ->
            container.matrixGroupId.isNullOrBlank()
        }
        // 重试时已结束的Job不会重跑，不回写映射也不打日志，避免与其实际执行时的依赖不一致
        val dependOnContainers = stageContainers.filter { container ->
            !container.status.isFinish() &&
                DependOnUtils.enableDependOn(container.controlOption.jobControlOption)
        }
        if (dependOnContainers.isEmpty()) {
            return
        }

        val stage = commandContext.stage
        // 解析时必须带上同 Stage 全部 Job，否则被依赖的 jobId 不在查找表中，映射会丢
        val jobs = stageContainers.map { container ->
            DependOnJob(
                jobId = container.jobId,
                containerId = container.containerId,
                jobControlOption = container.controlOption.jobControlOption
            )
        }
        // 审核参数在CheckPauseReviewStageCmd中只写入DB，未刷新到commandContext.variables
        val reviewVariables = stage.checkIn?.takeIf { it.manualTrigger == true }?.reviewParams
            ?.mapNotNull { param -> param.value?.let { param.key to it.toString() } }
            ?.toMap() ?: emptyMap()
        val unresolvedJobIds = try {
            DependOnUtils.initDependOn(
                jobs = jobs,
                params = commandContext.variables.plus(reviewVariables)
            )
        } catch (e: ErrorCodeException) {
            handleDependOnFailure(commandContext = commandContext, dependOnContainers = dependOnContainers, e = e)
            return
        }

        pipelineContainerService.batchUpdateControlOption(dependOnContainers)
        dependOnContainers.forEach { container ->
            val dependRel = container.controlOption.jobControlOption.dependOnContainerId2JobIds
            LOG.info(
                "ENGINE|${stage.buildId}|DEPEND_ON_INIT|s(${stage.stageId})|" +
                    "j(${container.containerId})|jobId=${container.jobId}|dependOn=$dependRel"
            )
            val startVmId = VMUtils.genStartVMTaskId(container.seq.toString())
            if (!dependRel.isNullOrEmpty()) {
                buildLogPrinter.addLine(
                    buildId = stage.buildId,
                    message = "Job[${container.jobId ?: container.containerId}] dependOn $dependRel",
                    tag = startVmId,
                    containerHashId = container.containerHashId,
                    executeCount = commandContext.executeCount,
                    jobId = null,
                    stepId = startVmId
                )
            }
            val notFoundJobIds = unresolvedJobIds[container.containerId]
            if (!notFoundJobIds.isNullOrEmpty()) {
                buildLogPrinter.addYellowLine(
                    buildId = stage.buildId,
                    message = "Job[${container.jobId ?: container.containerId}] dependOn $notFoundJobIds " +
                        "not found in current stage, ignored",
                    tag = startVmId,
                    containerHashId = container.containerHashId,
                    executeCount = commandContext.executeCount,
                    jobId = null,
                    stepId = startVmId
                )
            }
        }
    }

    /**
     * dependOn解析失败直接让Stage失败，避免异常抛出后Stage停留在排队中导致构建卡住
     */
    private fun handleDependOnFailure(
        commandContext: StageContext,
        dependOnContainers: List<PipelineBuildContainer>,
        e: ErrorCodeException
    ) {
        val stage = commandContext.stage
        val message = I18nUtil.getCodeLanMessage(
            messageCode = e.errorCode,
            params = e.params,
            defaultMessage = e.defaultMessage ?: "jobId dependOn resolve failed"
        )
        LOG.warn(
            "ENGINE|${stage.buildId}|DEPEND_ON_FAILED|s(${stage.stageId})|${e.errorCode}|$message"
        )
        buildLogPrinter.addErrorLine(
            buildId = stage.buildId,
            message = message,
            tag = stage.stageId,
            executeCount = commandContext.executeCount,
            jobId = null,
            stepId = null
        )
        // 构建失败原因由BuildEndControl从task错误信息汇总，需写到Job上才能透出；循环依赖时只写参与循环的Job
        val failedContainers = if (e.errorCode == ProcessMessageCode.ERROR_PIPELINE_DEPENDON_CYCLE) {
            val cycleJobIds = e.params?.toSet() ?: emptySet()
            dependOnContainers.filter { it.jobId in cycleJobIds }
        } else {
            dependOnContainers
        }
        failedContainers.forEach { container ->
            val startVMTask = pipelineTaskService.getBuildTask(
                projectId = container.projectId,
                buildId = container.buildId,
                taskId = VMUtils.genStartVMTaskId(container.seq.toString())
            ) ?: return@forEach
            pipelineTaskService.updateTaskStatus(
                task = startVMTask,
                userId = commandContext.event.userId,
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = message
            )
        }
        commandContext.buildStatus = BuildStatus.FAILED
        commandContext.latestSummary = "s(${stage.stageId}) dependOn failed"
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DependOnStageCmd::class.java)
    }
}
