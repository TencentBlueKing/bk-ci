package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserBcsClusterResource
import com.tencent.devops.environment.pojo.BcsCluster
import com.tencent.devops.environment.pojo.BcsImageInfo
import com.tencent.devops.environment.pojo.BcsVmModel
import com.tencent.devops.environment.pojo.ProjectInfo
import com.tencent.devops.environment.service.BcsClusterService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserBcsClusterResourceImpl @Autowired constructor(private val bcsClusterService: BcsClusterService) : UserBcsClusterResource {
    override fun getProjectInfo(userId: String, projectId: String): Result<ProjectInfo> {
        return Result(bcsClusterService.getProjectInfo(userId, projectId))
    }

    override fun getClusterList(): Result<List<BcsCluster>> {
        return Result(bcsClusterService.listBcsCluster())
    }

    override fun getImageList(userId: String, projectId: String): Result<List<BcsImageInfo>> {
        return Result(bcsClusterService.listBcsImageList())
    }

    override fun getVmModelList(userId: String, projectId: String): Result<List<BcsVmModel>> {
        return Result(bcsClusterService.listBcsVmModel())
    }
}