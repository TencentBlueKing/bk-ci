package com.tencent.devops.environment.resources


import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.TencentUserNodeResource
import com.tencent.devops.environment.pojo.imate.ImateListItem
import com.tencent.devops.environment.pojo.imate.ImportImageNodeData
import com.tencent.devops.environment.service.TencentNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TencentUserNodeResourceImpl @Autowired constructor(
    private val tencentNodeService: TencentNodeService
) : TencentUserNodeResource {
    override fun getUserImateList(
        userId: String,
        projectId: String
    ): Result<List<ImateListItem>> {
        return Result(tencentNodeService.getUserImateList(userId, projectId))
    }

    override fun batchImportImateNodes(
        userId: String,
        projectId: String,
        data: ImportImageNodeData
    ): Result<Boolean> {
        return Result(tencentNodeService.batchImportImateNodes(userId, projectId, data))
    }
}