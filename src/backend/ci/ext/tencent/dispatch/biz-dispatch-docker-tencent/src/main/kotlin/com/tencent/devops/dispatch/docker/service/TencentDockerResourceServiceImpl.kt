package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerRoutingType
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dispatch.devcloud.api.service.ServiceDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceMap
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.pojo.Pool
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsMap
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsShow
import com.tencent.devops.dispatch.docker.pojo.resource.UserDockerResourceOptionsVO
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentDockerResourceServiceImpl @Autowired constructor(
    private val client: Client,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : ExtDockerResourceService {

    private val logger = LoggerFactory.getLogger(TencentDockerResourceServiceImpl::class.java)

    override fun getDockerResourceConfigList(
        userId: String,
        projectId: String,
        buildType: String
    ): UserDockerResourceOptionsVO? {
        return if (buildType == BuildType.PUBLIC_DEVCLOUD.name) {
            getDevcloudResourceConfig(userId, projectId)
        } else {
            null
        }
    }

    override fun startExtDocker(
        event: PipelineAgentStartupEvent,
        containerPool: Pool,
        dockerRoutingType: DockerRoutingType,
        demoteFlag: Boolean
    ) {
        with(event) {
            pipelineEventDispatcher.dispatch(
                PipelineAgentStartupEvent(
                    source = "vmStartupTaskAtom",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    pipelineName = "",
                    userId = userId,
                    buildId = buildId,
                    buildNo = 0,
                    vmSeqId = containerId,
                    taskName = "",
                    os = "",
                    vmNames = vmNames,
                    channelCode = channelCode,
                    dispatchType = PublicDevCloudDispathcType(
                        image = JsonUtil.toJson(containerPool),
                        imageType = ImageType.THIRD,
                        performanceConfigId = "0"
                    ),
                    atoms = atoms,
                    executeCount = executeCount,
                    routeKeySuffix = if (!demoteFlag) DispatchRouteKeySuffix.DEVCLOUD.routeKeySuffix
                    else ".devcloud.public.demote",
                    containerId = containerId,
                    containerHashId = containerHashId,
                    customBuildEnv = customBuildEnv,
                    jobId = jobId
                )
            )
        }
    }

    private fun getDevcloudResourceConfig(
        userId: String,
        projectId: String
    ): UserDockerResourceOptionsVO {
        val result = client.get(ServiceDispatchDcResource::class).getDcPerformanceConfigList(
            userId = userId,
            projectId = projectId
        )

        if (result.status == 0 && result.data != null) {
            val dcUserPerformanceOptionsVO = result.data!!
            return UserDockerResourceOptionsVO(
                default = dcUserPerformanceOptionsVO.default,
                needShow = dcUserPerformanceOptionsVO.needShow,
                dockerResourceOptionsMaps = getDockerResourceOptionsMap(dcUserPerformanceOptionsVO.performanceMaps)
            )
        } else {
            val msg = result.message
            logger.error("[$projectId] get devcloud resourceConfig failed, msg: $msg")
            throw DockerServiceException(
                errorType = ErrorCodeEnum.END_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.END_VM_ERROR.errorCode,
                errorMsg = "Get devcloud resourceConfig failed, msg: $msg"
            )
        }
    }

    private fun getDockerResourceOptionsMap(performanceMaps: List<PerformanceMap>): List<DockerResourceOptionsMap> {
        val dockerResourceOptionsMaps = mutableListOf<DockerResourceOptionsMap>()
        performanceMaps.forEach {
            dockerResourceOptionsMaps.add(
                DockerResourceOptionsMap(
                    id = it.id,
                    dockerResourceOptionsShow = DockerResourceOptionsShow(
                        memory = it.performanceConfigVO.memory,
                        cpu = it.performanceConfigVO.cpu.toString(),
                        disk = it.performanceConfigVO.disk,
                        description = it.performanceConfigVO.description
                    )
            ))
        }

        return dockerResourceOptionsMaps
    }
}
