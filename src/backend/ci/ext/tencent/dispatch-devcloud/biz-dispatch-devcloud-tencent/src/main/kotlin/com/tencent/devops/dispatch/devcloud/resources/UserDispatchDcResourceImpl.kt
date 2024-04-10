package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.user.UserDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.service.DcPerformanceConfigService
import com.tencent.devops.dispatch.devcloud.service.DevcloudDebugService
import io.micrometer.core.annotation.Timed
import com.tencent.devops.common.api.pojo.Result

@RestResource
class UserDispatchDcResourceImpl constructor(
    private val devcloudDebugService: DevcloudDebugService,
    private val dcPerformanceConfigService: DcPerformanceConfigService
) : UserDispatchDcResource {

    @Timed
    override fun startDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?
    ): Result<DevCloudDebugResponse> {
        return Result(devcloudDebugService.startDebug(userId, "", pipelineId, buildId, vmSeqId))
    }

    override fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Result<Boolean> {
        return Result(devcloudDebugService.stopDebug(userId, pipelineId, containerName, vmSeqId))
    }

    override fun getDcPerformanceConfigList(userId: String, projectId: String): Result<UserPerformanceOptionsVO> {
        return Result(dcPerformanceConfigService.getDcPerformanceConfigList(userId, projectId))
    }
}
