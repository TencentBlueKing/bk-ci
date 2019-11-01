package com.tencent.devops.dispatch.service.dispatcher.docker

import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.dispatch.service.DockerHostBuildService
import com.tencent.devops.dispatch.service.dispatcher.BuildLessDispatcher
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.pojo.mq.PipelineBuildLessShutdownDispatchEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BuildLessDockerDispatcher @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val dockerHostBuildService: DockerHostBuildService
) : BuildLessDispatcher {
    override fun canDispatch(event: PipelineBuildLessStartupDispatchEvent) =
        event.dispatchType is DockerDispatchType

    override fun startUp(event: PipelineBuildLessStartupDispatchEvent) {
        val dockerDispatch = event.dispatchType as DockerDispatchType
        val dockerBuildVersion = dockerDispatch.dockerBuildVersion
        LogUtils.addLine(rabbitTemplate, event.buildId, "Start buildLessDocker $dockerBuildVersion for the build", "",  "",event.executeCount ?: 1)
        dockerHostBuildService.buildLessDockerHost(event)
    }

    override fun shutdown(event: PipelineBuildLessShutdownDispatchEvent) {
        dockerHostBuildService.finishBuildLessDockerHost(event.buildId, event.vmSeqId, event.userId, event.buildResult)
    }
}
