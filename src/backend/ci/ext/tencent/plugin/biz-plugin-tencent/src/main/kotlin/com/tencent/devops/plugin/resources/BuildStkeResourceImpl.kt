package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildStkeResource
import com.tencent.devops.plugin.pojo.stke.StkeType
import com.tencent.devops.plugin.pojo.stke.StkeUpdateParam
import com.tencent.devops.plugin.service.StkeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildStkeResourceImpl @Autowired constructor(
    private val stkeService: StkeService
) : BuildStkeResource {
    override fun updateDeployment(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String,
        updateParam: StkeUpdateParam
    ): Result<String> {
        return Result(
            stkeService.update(
                stkeType = stkeType,
                clusterName = clusterName,
                namespace = namespace,
                appsName = appsName,
                updateParam = updateParam
            )
        )
    }

    override fun getPodsStatus(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String
    ): Result<String> {
        return Result(
            stkeService.getPodsStatus(
                stkeType = stkeType,
                clusterName = clusterName,
                appsName = appsName,
                namespace = namespace
            )
        )
    }

    override fun getWorkload(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String
    ): Result<String> {
        return Result(
            stkeService.getWorkload(
                stkeType = stkeType,
                clusterName = clusterName,
                appsName = appsName,
                namespace = namespace
            )
        )
    }

    override fun getManagers(projectId: String): Result<String> {
        return Result(
            stkeService.getManagers(projectId = projectId)
        )
    }
}