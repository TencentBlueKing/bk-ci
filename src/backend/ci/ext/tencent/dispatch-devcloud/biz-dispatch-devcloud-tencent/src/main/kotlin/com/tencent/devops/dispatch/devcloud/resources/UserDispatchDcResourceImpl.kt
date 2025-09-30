package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.user.UserDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.service.DcPerformanceConfigService
import com.tencent.devops.dispatch.devcloud.service.DevcloudDebugService
import io.micrometer.core.annotation.Timed
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsV2
import com.tencent.devops.dispatch.devcloud.service.DcPerformanceConfigServiceV2

@RestResource
class UserDispatchDcResourceImpl constructor(
    private val devcloudDebugService: DevcloudDebugService,
    private val dcPerformanceConfigService: DcPerformanceConfigService,
    private val dcPerformanceConfigServiceV2: DcPerformanceConfigServiceV2
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

    override fun getDcPerformanceConfigListV2(
        userId: String,
        projectId: String,
        pipelineId: String,
        templateId: String
    ): Result<UserPerformanceOptionsV2> {
        return Result(dcPerformanceConfigServiceV2.getDcPerformanceConfigList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            templateId = templateId
        ))
    }

    override fun getDcPerformanceConfigInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        performanceUid: String
    ): Result<PerformanceData> {
        return Result(dcPerformanceConfigServiceV2.getDcPerformanceConfigInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            performanceUid = performanceUid
        ))
    }
}
