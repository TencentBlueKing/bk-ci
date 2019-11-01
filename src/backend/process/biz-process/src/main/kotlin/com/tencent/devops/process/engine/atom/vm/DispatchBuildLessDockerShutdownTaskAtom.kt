package com.tencent.devops.process.engine.atom.vm

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.EnvControlTaskType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DispatchBuildLessDockerShutdownTaskAtom @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : IAtomTask<NormalContainer> {
    override fun getParamElement(task: PipelineBuildTask): NormalContainer {
        return JsonUtil.mapTo(task.taskParams, NormalContainer::class.java)
    }

    private val logger = LoggerFactory.getLogger(DispatchBuildLessDockerShutdownTaskAtom::class.java)

    override fun execute(task: PipelineBuildTask, param: NormalContainer, runVariables: Map<String, String>): AtomResponse {

        val vmSeqId = task.containerId

        val buildId = task.buildId

        val projectId = task.projectId

        val pipelineId = task.pipelineId

        pipelineEventDispatcher.dispatch(
            PipelineBuildLessShutdownDispatchEvent(
                source = "shutdownVMTaskAtom",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = task.starter,
                buildId = buildId,
                vmSeqId = vmSeqId,
                buildResult = true
            )
        )
        logger.info("[$buildId]|SHUTDOWN_VM|stageId=${task.stageId}|container=${task.containerId}|vmSeqId=$vmSeqId")
        return AtomResponse(BuildStatus.SUCCEED)
    }

    companion object {
        /**
         * 生成构建任务
         */
        fun makePipelineBuildTasks(
            projectId: String,
            pipelineId: String,
            buildId: String,
            stageId: String,
            container: Container,
            containerSeq: Int,
            taskSeq: Int,
            userId: String
        ): List<PipelineBuildTask> {

            val list: MutableList<PipelineBuildTask> = mutableListOf()

            val containerId = container.id!!
            val containerType = container.getClassType()
            val endTaskSeq = VMUtils.genVMSeq(containerSeq, taskSeq - 1)

            // end-1xxx 无后续任务的结束节点
            list.add(
                PipelineBuildTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerHashId = container.containerId ?: "",
                    containerType = containerType,
                    taskSeq = endTaskSeq,
                    taskId = VMUtils.genEndPointTaskId(endTaskSeq),
                    taskName = "Wait_Finish_Job#$containerId(N)",
                    taskType = EnvControlTaskType.NORMAL.name,
                    taskAtom = "",
                    status = BuildStatus.QUEUE,
                    taskParams = mutableMapOf(),
                    executeCount = 1,
                    starter = userId,
                    approver = null,
                    subBuildId = null,
                    additionalOptions = null
                )
            )

            // stopVM-1xxx 停止虚拟机节点
            val stopVMTaskSeq = VMUtils.genVMSeq(containerSeq, taskSeq)
            val taskParams = container.genTaskParams()
            taskParams["elements"] = emptyList<Element>() // elements可能过多导致存储问题
            list.add(
                PipelineBuildTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerHashId = container.containerId ?: "",
                    containerType = containerType,
                    taskSeq = stopVMTaskSeq,
                    taskId = VMUtils.genStopVMTaskId(stopVMTaskSeq),
                    taskName = "Clean_Job#$containerId(N)",
                    taskType = EnvControlTaskType.NORMAL.name,
                    taskAtom = AtomUtils.parseAtomBeanName(DispatchBuildLessDockerShutdownTaskAtom::class.java),
                    status = BuildStatus.QUEUE,
                    taskParams = taskParams,
                    executeCount = 1,
                    starter = userId,
                    approver = null,
                    subBuildId = null,
                    additionalOptions = null
                )
            )

            return list
        }
    }
}
