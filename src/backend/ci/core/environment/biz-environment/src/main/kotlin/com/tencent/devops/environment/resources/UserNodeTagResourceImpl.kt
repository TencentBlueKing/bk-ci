package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserNodeTagResource
import com.tencent.devops.environment.pojo.NodeTag
import com.tencent.devops.environment.pojo.NodeTagReq
import com.tencent.devops.environment.pojo.NodeTagUpdateReq
import com.tencent.devops.environment.pojo.UpdateNodeTag
import com.tencent.devops.environment.service.NodeTagService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserNodeTagResourceImpl @Autowired constructor(
    private val nodeTagService: NodeTagService
) : UserNodeTagResource {
    override fun createTag(
        userId: String,
        projectId: String,
        data: NodeTagReq
    ): Result<Boolean> {
        nodeTagService.createTag(
            userId = userId,
            projectId = projectId,
            tagKey = data.tagKeyName,
            tagValues = data.tagValues,
            allowMulValue = data.tagAllowMulValue
        )
        return Result(true)
    }

    override fun fetchTag(
        userId: String,
        projectId: String
    ): Result<List<NodeTag>> {
        return Result(nodeTagService.fetchTagAndNodeCount(projectId))
    }

    override fun editTag(
        userId: String,
        projectId: String,
        data: UpdateNodeTag
    ): Result<Boolean> {
        nodeTagService.addNodeTag(userId = userId, projectId = projectId, data = data)
        return Result(true)
    }

    override fun deleteTag(
        userId: String,
        projectId: String,
        tagKeyId: Long,
        tagValueId: Long?
    ): Result<Boolean> {
        nodeTagService.deleteTag(userId = userId, projectId = projectId, tagKey = tagKeyId, tagValueId = tagValueId)
        return Result(true)
    }

    override fun updateTag(
        userId: String,
        projectId: String,
        data: NodeTagUpdateReq
    ): Result<Boolean> {
        nodeTagService.updateTag(
            userId = userId,
            projectId = projectId,
            data = data
        )
        return Result(true)
    }

    override fun batchEditTag(
        userId: String,
        projectId: String,
        data: List<UpdateNodeTag>
    ): Result<Boolean> {
        nodeTagService.batchAddNodeTag(userId = userId, projectId = projectId, data = data)
        return Result(true)
    }
}