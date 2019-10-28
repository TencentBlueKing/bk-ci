package com.tencent.devops.dockerhost.listener

import com.tencent.devops.dockerhost.service.DockerHostBuildLessService
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerShutdownEvent
import org.slf4j.LoggerFactory

/**
 * 无构建环境的容器停止消息
 * @version 1.0
 */
class BuildLessStopListener(
    private val dockerHostBuildLessService: DockerHostBuildLessService
) {

    private val logger = LoggerFactory.getLogger(BuildLessStopListener::class.java)

    fun handleMessage(event: PipelineBuildLessDockerShutdownEvent) {
        logger.info("[${event.buildId}]| Stop container(${event.dockerContainerId})")
        dockerHostBuildLessService.stopContainer(event.buildId, event.dockerContainerId)
    }
}
