package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
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
    private val buildLogPrinter: BuildLogPrinter
) : StageCmd {

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.buildStatus.isReadyToRun() &&
            commandContext.containers.isNotEmpty()
    }

    override fun execute(commandContext: StageContext) {
        val stageContainers = commandContext.containers.filter { container ->
            container.matrixGroupId.isNullOrBlank()
        }
        val dependOnContainers = stageContainers.filter { container ->
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
        try {
            DependOnUtils.initDependOn(
                jobs = jobs,
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
            dependOnContainers.forEach { container ->
                addJobLog(
                    container = container,
                    message = message,
                    executeCount = commandContext.executeCount,
                    error = true
                )
            }
            commandContext.buildStatus = BuildStatus.FAILED
            commandContext.latestSummary = "s(${stage.stageId}) dependOn cycle"
            commandContext.cmdFlowState = CmdFlowState.FINALLY
            return
        }

        pipelineContainerService.batchUpdateControlOption(dependOnContainers)
        dependOnContainers.forEach { container ->
            val dependJobIds = container.controlOption.jobControlOption.dependOnContainerId2JobIds?.values
            LOG.info(
                "ENGINE|${stage.buildId}|DEPEND_ON_INIT|s(${stage.stageId})|" +
                    "j(${container.containerId})|jobId=${container.jobId}|dependOn=$dependJobIds"
            )
            if (!dependJobIds.isNullOrEmpty()) {
                addJobLog(
                    container = container,
                    message = "Job[${container.jobId ?: container.containerId}] dependOn $dependJobIds",
                    executeCount = commandContext.executeCount,
                    error = false
                )
            }
        }
    }

    private fun addJobLog(
        container: PipelineBuildContainer,
        message: String,
        executeCount: Int,
        error: Boolean
    ) {
        val startVmId = VMUtils.genStartVMTaskId(container.seq.toString())
        if (error) {
            buildLogPrinter.addErrorLine(
                buildId = container.buildId,
                message = message,
                tag = startVmId,
                containerHashId = container.containerHashId,
                executeCount = executeCount,
                jobId = null,
                stepId = startVmId
            )
        } else {
            buildLogPrinter.addLine(
                buildId = container.buildId,
                message = message,
                tag = startVmId,
                containerHashId = container.containerHashId,
                executeCount = executeCount,
                jobId = null,
                stepId = startVmId
            )
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DependOnStageCmd::class.java)
    }
}
