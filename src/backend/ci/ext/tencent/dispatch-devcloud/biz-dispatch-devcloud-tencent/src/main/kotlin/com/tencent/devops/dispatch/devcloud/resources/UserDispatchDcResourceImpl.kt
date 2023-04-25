package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.user.UserDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.service.DispatchDevcloudService
import io.micrometer.core.annotation.Timed

@RestResource
class UserDispatchDcResourceImpl constructor(
    private val dispatchDevcloudService: DispatchDevcloudService
) : UserDispatchDcResource {

    @Timed
    override fun startDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?
    ): Result<DevCloudDebugResponse> {
        return Result(dispatchDevcloudService.startDebug(userId, "", pipelineId, buildId, vmSeqId))
    }

    override fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Result<Boolean> {
        return Result(dispatchDevcloudService.stopDebug(userId, pipelineId, containerName, vmSeqId))
    }

    override fun getDcPerformanceConfigList(userId: String, projectId: String): Result<UserPerformanceOptionsVO> {
        return Result(dispatchDevcloudService.getDcPerformanceConfigList(userId, projectId))
    }
}
