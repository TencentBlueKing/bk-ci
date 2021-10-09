package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CheckPauseAtomStageCmd @Autowired constructor(
    val pipelineBuildService: PipelineRuntimeService
) : StageCmd{
    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.stage.controlOption?.finally != true &&
            commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            !commandContext.buildStatus.isFinish()
    }

    override fun execute(commandContext: StageContext) {
        commandContext.containers.forEach {
            val pauseTask = pipelineBuildService.listContainerBuildTasks(
                buildId = commandContext.stage.buildId,
                containerId = it.containerId,
                buildStatusSet = setOf(BuildStatus.PAUSE)
            )
            if (pauseTask.isNotEmpty()) {
                logger.info("CheckPauseAtomStageCmd ${commandContext.stage.buildId} has pause task")
                commandContext.buildStatus = BuildStatus.PAUSE
                commandContext.cmdFlowState = CmdFlowState.FINALLY
            }
        }
    }

    companion object{
        val logger = LoggerFactory.getLogger(CheckPauseAtomStageCmd::class.java)
    }
}
