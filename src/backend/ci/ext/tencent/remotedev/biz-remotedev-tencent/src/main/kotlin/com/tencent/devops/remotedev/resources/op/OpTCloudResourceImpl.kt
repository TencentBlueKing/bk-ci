package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpTCloudResource
import com.tencent.devops.remotedev.pojo.tcloud.ProjectCfsData
import com.tencent.devops.remotedev.service.tcloud.TCloudCfsService
import com.tencentcloudapi.common.profile.Region
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpTCloudResourceImpl @Autowired constructor(
    private val tCloudCfsService: TCloudCfsService
) : OpTCloudResource {
    override fun fetchProjectCfs(
        userId: String,
        page: Int,
        pageSize: Int
    ): Result<List<ProjectCfsData>> {
        return Result(tCloudCfsService.projectCfsList(page, pageSize))
    }

    override fun fetchCfsRegion(userId: String): Result<List<String>> {
        return Result(Region.values().map { it.value })
    }

    override fun addProjectCfs(userId: String, data: ProjectCfsData): Result<Boolean> {
        if (!Region.values().any { it.value == data.region }) {
            throw RuntimeException("region ${data.region} not support")
        }
        tCloudCfsService.addProjectCfsId(
            projectId = data.projectId,
            cfsId = data.cfsId,
            region = data.region
        )
        return Result(true)
    }

    override fun deleteProjectCfs(userId: String, projectId: String, cfsId: String) {
        tCloudCfsService.deleteProjectCfs(projectId, cfsId)
    }
}
