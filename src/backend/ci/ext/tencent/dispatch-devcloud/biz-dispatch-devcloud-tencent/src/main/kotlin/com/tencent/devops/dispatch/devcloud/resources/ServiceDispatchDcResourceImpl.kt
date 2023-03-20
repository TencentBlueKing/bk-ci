package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.service.ServiceDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.service.DispatchDevcloudService

@RestResource
class ServiceDispatchDcResourceImpl constructor(
    private val dispatchDevcloudService: DispatchDevcloudService
) : ServiceDispatchDcResource {

    override fun getDcPerformanceConfigList(userId: String, projectId: String): Result<UserPerformanceOptionsVO> {
        return Result(dispatchDevcloudService.getDcPerformanceConfigList(userId, projectId))
    }

    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?
    ): Result<DevCloudDebugResponse> {
        return Result(dispatchDevcloudService.startDebug(userId, projectId, pipelineId, buildId, vmSeqId))
    }

    override fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Result<Boolean> {
        return Result(dispatchDevcloudService.stopDebug(userId, pipelineId, containerName, vmSeqId))
    }
}
