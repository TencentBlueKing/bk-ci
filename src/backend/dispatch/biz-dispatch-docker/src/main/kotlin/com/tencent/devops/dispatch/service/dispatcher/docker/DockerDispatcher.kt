package com.tencent.devops.dispatch.service.dispatcher.docker

import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.dispatch.service.DockerHostBuildService
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DockerDispatcher @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val dockerHostBuildService: DockerHostBuildService
) : Dispatcher {
    override fun canDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) =
        pipelineAgentStartupEvent.dispatchType is DockerDispatchType

    override fun startUp(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        val dockerDispatch = pipelineAgentStartupEvent.dispatchType as DockerDispatchType
        LogUtils.addLine(
            rabbitTemplate,
            pipelineAgentStartupEvent.buildId,
            "Start docker ${dockerDispatch.dockerBuildVersion} for the build",
            "",
            pipelineAgentStartupEvent.executeCount ?: 1
        )
        dockerHostBuildService.dockerHostBuild(pipelineAgentStartupEvent)
    }

    override fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        dockerHostBuildService.finishDockerBuild(
            pipelineAgentShutdownEvent.buildId,
            pipelineAgentShutdownEvent.vmSeqId,
            pipelineAgentShutdownEvent.buildResult
        )
    }

//    override fun canDispatch(buildMessage: PipelineBuildMessage) =
//        buildMessage.dispatchType.buildType == BuildType.DOCKER
//
//    override fun build(buildMessage: PipelineBuildMessage) {
//        val dockerDispatch = buildMessage.dispatchType as DockerDispatchType
//        LogUtils.addLine(client, buildMessage.buildId, "Start docker ${dockerDispatch.dockerBuildVersion} for the build", "", buildMessage.executeCount ?: 1)
//        dockerHostBuildService.dockerHostBuild(buildMessage)
//    }
//
//    override fun finish(buildFinishMessage: PipelineFinishMessage) {
//        dockerHostBuildService.finishDockerBuild(
//            buildFinishMessage.buildId,
//            buildFinishMessage.vmSeqId,
//            buildFinishMessage.buildResult
//        )
//    }
}
