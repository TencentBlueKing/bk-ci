package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
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
    ): Result<Boolean> {
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
        clusterName: String,
        namespace: String,
        appsName: String
    ): Result<Boolean> {
        return Result(
            stkeService.getPodsStatus(
                clusterName = "cls-owj0g590",
                appsName = "ddtest-buklj2kuz0w0",
                namespace = "ns-ddtest-production"
            )
        )
    }
}