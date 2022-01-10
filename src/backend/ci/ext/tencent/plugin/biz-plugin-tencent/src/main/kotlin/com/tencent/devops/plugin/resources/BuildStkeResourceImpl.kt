package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildStkeResource
import com.tencent.devops.plugin.pojo.stke.ConfigMapData
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
        val value = stkeService.update(
            stkeType = stkeType,
            clusterName = clusterName,
            namespace = namespace,
            appsName = appsName,
            updateParam = updateParam
        )
        return if (value == null) {
            Result(
                status = 0,
                data = "",
                message = "STKE_ERROR"
            )
        } else {
            Result(value)
        }
    }

    override fun getPodsStatus(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String
    ): Result<String> {
        val value = stkeService.getPodsStatus(
            stkeType = stkeType,
            clusterName = clusterName,
            namespace = namespace,
            appsName = appsName
        )
        return if (value == null) {
            Result(
                status = 0,
                data = "",
                message = "STKE_ERROR"
            )
        } else {
            Result(value)
        }
    }

    override fun getWorkload(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String
    ): Result<String> {
        val value = stkeService.getWorkload(
            stkeType = stkeType,
            clusterName = clusterName,
            namespace = namespace,
            appsName = appsName
        )
        return if (value == null) {
            Result(
                status = 0,
                data = "",
                message = "STKE_ERROR"
            )
        } else {
            Result(value)
        }
    }

    override fun getManagers(projectId: String): Result<String> {
        val value = stkeService.getManagers(
                projectId = projectId
            )
        return if (value == null) {
            Result(
                status = 0,
                data = "",
                message = "STKE_ERROR"
            )
        } else {
            Result(value)
        }
    }

    override fun updateConfigMap(
        clusterName: String,
        namespace: String,
        configMapName: String,
        configMapData: List<ConfigMapData>
    ): Result<String> {
        val value = stkeService.updateConfigMap(
            clusterName = clusterName,
            namespace = namespace,
            configMapName = configMapName,
            configMapData = configMapData
        )
        return if (value == null) {
            Result(
                status = 0,
                data = "",
                message = "STKE_ERROR"
            )
        } else {
            Result(value)
        }
    }

    override fun updateWorkLoad(
        stkeType: StkeType,
        clusterName: String,
        namespace: String,
        appsName: String,
        operator: String,
        buildId: String
    ): Result<String> {
        val value = stkeService.updateWorkLoad(
            clusterName = clusterName,
            namespace = namespace,
            stkeType = stkeType,
            workLoadName = appsName,
            operator = operator,
            buildId = buildId
        )
        return if (value == null) {
            Result(
                status = 0,
                data = "",
                message = "STKE_ERROR"
            )
        } else {
            Result(value)
        }
    }
}