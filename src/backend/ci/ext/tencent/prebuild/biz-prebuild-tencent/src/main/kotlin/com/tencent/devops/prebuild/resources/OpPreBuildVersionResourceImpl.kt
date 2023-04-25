package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.prebuild.api.OpPreBuildVersionResource
import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.prebuild.service.PreBuildService
import com.tencent.devops.project.pojo.Result
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPreBuildVersionResourceImpl @Autowired constructor(
    private val preBuildService: PreBuildService
) : OpPreBuildVersionResource {
    override fun creatIdeVersion(prePluginVersion: PrePluginVersion): Result<Boolean> {
        return Result(preBuildService.creatPluginVersion(prePluginVersion))
    }

    override fun updateIdeVersion(prePluginVersion: PrePluginVersion): Result<Boolean> {
        return Result(preBuildService.updatePluginVersion(prePluginVersion))
    }

    override fun deleteIdeVersion(version: String): Result<Boolean> {
        return Result(preBuildService.deletePluginVersion(version))
    }

    override fun getIdeVersion(): Result<List<PrePluginVersion>> {
        return Result(preBuildService.getPluginVersionList())
    }
}