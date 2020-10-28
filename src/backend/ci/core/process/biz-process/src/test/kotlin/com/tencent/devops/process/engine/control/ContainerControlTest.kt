package com.tencent.devops.process.engine.control

import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.log.utils.LogMQEventDispatcher
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import org.junit.Assert
import org.junit.Test
import java.time.LocalDateTime

class ContainerControlTest {

    private val buildLogPrinter: BuildLogPrinter = BuildLogPrinter(LogMQEventDispatcher(mock()))
    private val redisOperation: RedisOperation = RedisOperation(mock())
    private val pipelineRuntimeService: PipelineRuntimeService = mock()

    private val containerControl = ContainerControl(
        buildLogPrinter = buildLogPrinter,
        redisOperation = redisOperation,
        pipelineEventDispatcher = mock(),
        pipelineBuildDetailService = mock(),
        pipelineRuntimeService = mock(),
        buildVariableService = mock(),
        mutexControl = MutexControl(buildLogPrinter = buildLogPrinter, redisOperation = redisOperation, pipelineRuntimeService = pipelineRuntimeService),
        dependOnControl = mock()
    )

    private val projectId = "devops1"
    private val buildId = "b-12345678901234567890123456789012"
    private val pipelineId = "p-12345678901234567890123456789012"
    private val stageId = "stage-1"

    @Test
    fun checkVMContainerIfAllSkip() {

        val vmContainer = genVmContainer()
        val variables = mutableMapOf<String, String>()

        val emptyTask = mutableListOf<PipelineBuildTask>()
        var result = containerControl.checkIfAllSkip(buildId = buildId, stageId = stageId, container = vmContainer, containerTaskList = emptyTask, variables = variables)
        Assert.assertTrue(result)

        val taskId = "e-12345678901234567890123456789012"
        val oneTask = mutableListOf(genTask(taskId = taskId, vmContainer = vmContainer))

        val conditionStageSucc = JobControlOption(enable = false, timeout = 600, runCondition = JobRunCondition.STAGE_RUNNING)
        val disabledVmContainer = genVmContainer(jobControlOption = conditionStageSucc)
        result = containerControl.checkIfAllSkip(buildId = buildId, stageId = stageId, container = disabledVmContainer, containerTaskList = oneTask, variables = variables)
        Assert.assertTrue(result)
    }

    private fun genVmContainer(jobControlOption: JobControlOption? = null): PipelineBuildContainer {
        val containerId = "c-1"
        val status = BuildStatus.RUNNING
        val vmContainerType = "vmBuild"
        val startTime = LocalDateTime.now().minusMinutes(10)
        val containerCost = 5
        return PipelineBuildContainer(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            seq = 1,
            containerType = vmContainerType,
            status = status,
            startTime = startTime,
            endTime = null,
            controlOption = PipelineBuildContainerControlOption(
                jobControlOption = jobControlOption ?: JobControlOption(enable = true, timeout = 600, runCondition = JobRunCondition.STAGE_RUNNING),
                mutexGroup = null
            ),
            cost = containerCost
        )
    }

    private fun genTask(taskId: String, vmContainer: PipelineBuildContainer, elementAdditionalOptions: ElementAdditionalOptions? = null): PipelineBuildTask {
        with(vmContainer) {
            return PipelineBuildTask(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId,
                containerType = vmContainer.containerType, containerId = containerId,
                startTime = startTime?.plusSeconds(vmContainer.cost.toLong()), status = status, stageId = stageId,
                taskId = taskId, taskAtom = "", taskName = "Demo", taskParams = mutableMapOf(),
                taskSeq = 1, taskType = vmContainer.containerType, starter = "user1", containerHashId = containerId, approver = null, subBuildId = null,
                additionalOptions = elementAdditionalOptions ?: elementAdditionalOptions()
            )
        }
    }

    private fun elementAdditionalOptions(enable: Boolean = true, runCondition: RunCondition = RunCondition.PRE_TASK_SUCCESS, customVariables: MutableList<NameAndValue>? = null): ElementAdditionalOptions {
        return ElementAdditionalOptions(
            enable = enable,
            continueWhenFailed = false,
            retryWhenFailed = false,
            runCondition = runCondition, customVariables = customVariables,
            retryCount = 0,
            timeout = 100,
            otherTask = null,
            customCondition = null
        )
    }
}