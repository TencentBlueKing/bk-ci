package com.tencent.devops.dockerhost.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.dispatch.AlertApi
import com.tencent.devops.dockerhost.exception.ContainerException
import com.tencent.devops.dockerhost.exception.NoSuchImageException
import com.tencent.devops.dockerhost.services.DockerHostBuildService
import com.tencent.devops.dockerhost.utils.CommonUtils
import com.tencent.devops.dockerhost.utils.MAX_CONTAINER_NUM
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceIdcDockerHostResourceImpl @Autowired constructor(private val dockerHostBuildService: DockerHostBuildService) : ServiceIdcDockerHostResource {

    private val alertApi: AlertApi = AlertApi()

    override fun startBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<String> {
        try {
            val containerNum = dockerHostBuildService.getContainerNum()
            if (containerNum >= MAX_CONTAINER_NUM) {
                logger.warn("Too many containers in this host, break to start build.")
                alertApi.alert(AlertLevel.HIGH.name, "Docker构建机运行的容器太多", "Docker构建机运行的容器太多, " +
                        "母机IP:${CommonUtils.getInnerIP()}， 容器数量: $containerNum")
                return Result(1, "Docker构建机运行的容器太多，母机IP:${CommonUtils.getInnerIP()}，容器数量: $containerNum")
            }
            logger.warn("Create container, dockerStartBuildInfo: $dockerHostBuildInfo")

            val containerId = dockerHostBuildService.createContainer(dockerHostBuildInfo)
            dockerHostBuildService.log(dockerHostBuildInfo.buildId, "构建环境启动成功，等待Agent启动...")
            return Result(containerId)
        } catch (e: NoSuchImageException) {
            logger.error("Create container container failed, no such image. pipelineId: ${dockerHostBuildInfo.pipelineId}, vmSeqId: ${dockerHostBuildInfo.vmSeqId}, err: ${e.message}")
            dockerHostBuildService.log(dockerHostBuildInfo.buildId, "构建环境启动失败，镜像不存在, 镜像:${dockerHostBuildInfo.imageName}")
            return Result(2, e.message, "")
        } catch (e: ContainerException) {
            logger.error("Create container failed, rollback build. buildId: ${dockerHostBuildInfo.buildId}, vmSeqId: ${dockerHostBuildInfo.vmSeqId}")
            dockerHostBuildService.log(dockerHostBuildInfo.buildId, "构建环境启动失败，错误信息:${e.message}")
            return Result(1, e.message, "")
        }
    }

    override fun endBuild(dockerHostBuildInfo: DockerHostBuildInfo): Result<Boolean> {
        logger.warn("Stop the container, containerId: ${dockerHostBuildInfo.containerId}")
        dockerHostBuildService.stopContainer(dockerHostBuildInfo)

        return Result(true)
    }

    override fun getContainerCount(): Result<Int> {
        return Result(0, "success", dockerHostBuildService.getContainerNum())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceIdcDockerHostResourceImpl::class.java)
    }
}