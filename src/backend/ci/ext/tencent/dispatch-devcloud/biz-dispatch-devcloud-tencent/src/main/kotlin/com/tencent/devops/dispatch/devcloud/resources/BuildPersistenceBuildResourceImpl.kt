package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.builds.BuildPersistenceBuildResource
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildInfo
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildWithStatus
import com.tencent.devops.dispatch.devcloud.service.PersistenceBuildService

@RestResource
class BuildPersistenceBuildResourceImpl constructor(
    private val persistenceBuildService: PersistenceBuildService
) : BuildPersistenceBuildResource {
    override fun startBuild(projectId: String, agentId: String): Result<PersistenceBuildInfo?> {
        return Result(persistenceBuildService.startBuild(projectId, agentId))
    }

    override fun workerBuildFinish(
        projectId: String,
        agentId: String,
        buildInfo: PersistenceBuildWithStatus
    ): Result<Boolean> {
        persistenceBuildService.workerBuildFinish(projectId, agentId, buildInfo)
        return Result(true)
    }
}
