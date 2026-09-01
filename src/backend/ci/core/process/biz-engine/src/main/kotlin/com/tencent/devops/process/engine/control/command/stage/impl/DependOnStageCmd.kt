package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.utils.DependOnJob
import com.tencent.devops.process.utils.DependOnUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage 首次启动时用运行时变量重算 Job dependOn，并校验循环依赖
 */
@Service
class DependOnStageCmd(
    private val pipelineContainerService: PipelineContainerService,
    private val buildLogPrinter: BuildLogPrinter
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.buildStatus.isReadyToRun() &&
            commandContext.containers.isNotEmpty()
    }

    override fun execute(commandContext: StageContext) {
        val dependOnContainers = commandContext.containers.filter { container ->
            container.matrixGroupId.isNullOrBlank() &&
                DependOnUtils.enableDependOn(container.controlOption.jobControlOption)
        }
        if (dependOnContainers.isEmpty()) {
            return
        }

        val stage = commandContext.stage
        try {
            DependOnUtils.initDependOn(
                jobs = dependOnContainers.map { container ->
                    DependOnJob(
                        jobId = container.jobId,
                        containerId = container.containerId,
                        jobControlOption = container.controlOption.jobControlOption
                    )
                },
                params = commandContext.variables
            )
        } catch (e: ErrorCodeException) {
            if (e.errorCode != ProcessMessageCode.ERROR_PIPELINE_DEPENDON_CYCLE) {
                throw e
            }
            val message = I18nUtil.getCodeLanMessage(
                messageCode = e.errorCode,
                params = e.params,
                defaultMessage = e.defaultMessage ?: "jobId circular dependency"
            )
            LOG.warn(
                "ENGINE|${stage.buildId}|DEPEND_ON_CYCLE|s(${stage.stageId})|$message"
            )
            buildLogPrinter.addErrorLine(
                buildId = stage.buildId,
                message = message,
                tag = stage.stageId,
                executeCount = commandContext.executeCount,
                jobId = null,
                stepId = null
            )
            commandContext.buildStatus = BuildStatus.FAILED
            commandContext.latestSummary = "s(${stage.stageId}) dependOn cycle"
            commandContext.cmdFlowState = CmdFlowState.FINALLY
            return
        }

        pipelineContainerService.batchUpdate(transactionContext = null, containerList = dependOnContainers)
        dependOnContainers.forEach { container ->
            val dependRel = container.controlOption.jobControlOption.dependOnContainerId2JobIds
            LOG.info(
                "ENGINE|${stage.buildId}|DEPEND_ON_INIT|s(${stage.stageId})|" +
                    "j(${container.containerId})|jobId=${container.jobId}|dependOn=$dependRel"
            )
            if (!dependRel.isNullOrEmpty()) {
                buildLogPrinter.addLine(
                    buildId = stage.buildId,
                    message = "Job[${container.jobId ?: container.containerId}] dependOn $dependRel",
                    tag = stage.stageId,
                    containerHashId = container.containerHashId,
                    executeCount = commandContext.executeCount,
                    jobId = container.jobId,
                    stepId = null
                )
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DependOnStageCmd::class.java)
    }
}
