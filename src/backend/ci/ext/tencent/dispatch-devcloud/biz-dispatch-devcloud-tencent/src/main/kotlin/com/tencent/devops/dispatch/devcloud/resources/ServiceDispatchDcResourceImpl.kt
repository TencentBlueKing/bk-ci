package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.service.ServiceDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.DestroyContainerReq
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.service.DcPerformanceConfigService
import com.tencent.devops.dispatch.devcloud.service.DevcloudDebugService
import com.tencent.devops.dispatch.devcloud.service.PersistenceBuildService

@RestResource
class ServiceDispatchDcResourceImpl constructor(
    private val devcloudDebugService: DevcloudDebugService,
    private val dcPerformanceConfigService: DcPerformanceConfigService,
    private val persistenceBuildService: PersistenceBuildService
) : ServiceDispatchDcResource {

    override fun getDcPerformanceConfigList(userId: String, projectId: String): Result<UserPerformanceOptionsVO> {
        return Result(dcPerformanceConfigService.getDcPerformanceConfigList(userId, projectId))
    }

    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?
    ): Result<DevCloudDebugResponse> {
        return Result(devcloudDebugService.startDebug(userId, projectId, pipelineId, buildId, vmSeqId))
    }

    override fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Result<Boolean> {
        return Result(devcloudDebugService.stopDebug(userId, pipelineId, containerName, vmSeqId))
    }

    override fun destroyContainer(userId: String, destroyContainerReq: DestroyContainerReq): Result<Boolean> {
        return persistenceBuildService.destroyPersistenceContainer(userId, destroyContainerReq)
    }
}
