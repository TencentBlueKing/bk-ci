package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.devcloud.api.service.ServiceDispatchDcResource
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.service.debug.ExtDebugService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DevcloudDebugServiceImpl @Autowired constructor(
    private val client: Client
) : ExtDebugService {

    private val logger = LoggerFactory.getLogger(DevcloudDebugServiceImpl::class.java)

    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String
    ): String? {
        val devCloudDebugResult = client.get(ServiceDispatchDcResource::class).startDebug(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            buildId = buildId
        )

        if (devCloudDebugResult.code != 0) {
            val msg = devCloudDebugResult.message
            logger.error("[$pipelineId] get devcloud debugUrl failed, msg: $msg")
            throw DockerServiceException(
                errorType = ErrorCodeEnum.START_VM_FAIL.errorType,
                errorCode = ErrorCodeEnum.START_VM_FAIL.errorCode,
                errorMsg = msg ?: ""
            )
        }

        return devCloudDebugResult.data?.websocketUrl ?: ""
    }

    override fun getWebsocketUrl(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        containerId: String
    ): String? {
        return null
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Boolean {
        return client.get(ServiceDispatchDcResource::class).stopDebug(
            userId = userId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            containerName = containerName
        ).data ?: false
    }
}
