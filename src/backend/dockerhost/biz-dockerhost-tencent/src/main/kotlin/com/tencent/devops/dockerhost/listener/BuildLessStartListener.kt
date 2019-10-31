package com.tencent.devops.dockerhost.listener

import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.service.DockerHostBuildLessService
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.process.pojo.mq.PipelineBuildLessDockerStartupEvent
import org.slf4j.LoggerFactory

/**
 * 无构建环境的容器启动消息
 * @version 1.0
 */
class BuildLessStartListener(
    private val dockerHostBuildLessService: DockerHostBuildLessService
) {

    private val alertApi: AlertApi =
        AlertApi()

    private val maxRunningContainerNum = 200

    private val logger = LoggerFactory.getLogger(BuildLessStartListener::class.java)

    fun handleMessage(event: PipelineBuildLessDockerStartupEvent) {

        logger.info("[${event.buildId}]|Create container, event: $event")

        val containerId = try {
            val containerNum = dockerHostBuildLessService.getContainerNum()
            if (containerNum >= maxRunningContainerNum) {
                logger.warn("[${event.buildId}]|Too many containers in this host, break to start build.")
                dockerHostBuildLessService.retryDispatch(event)
                alertApi.alert(
                    AlertLevel.HIGH.name, "Docker无构建环境运行的容器太多", "Docker无构建环境运行的容器太多, " +
                        "母机IP:${CommonUtils.getInnerIP()}， 容器数量: $containerNum"
                )
                return
            }
            dockerHostBuildLessService.createContainer(event)
        } catch (e: ContainerException) {
            logger.error("[${event.buildId}]|Create container failed, rollback build. buildId: ${event.buildId}, vmSeqId: ${event.vmSeqId}")
            dockerHostBuildLessService.retryDispatch(event)
            return
        }
        logger.info("[${event.buildId}]|Create container=$containerId")
        dockerHostBuildLessService.reportContainerId(event.buildId, event.vmSeqId, containerId)
    }
}
