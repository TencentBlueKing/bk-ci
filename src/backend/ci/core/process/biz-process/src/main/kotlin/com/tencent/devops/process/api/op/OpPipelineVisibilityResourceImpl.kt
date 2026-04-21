package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineVisibility
import com.tencent.devops.process.engine.service.PipelineVisibilityService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineVisibilityResourceImpl @Autowired constructor(
    private val pipelineVisibilityService: PipelineVisibilityService
) : OpPipelineVisibilityResource {

    override fun addVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        visibilityList: List<PipelineVisibility>
    ): Result<Boolean> {
        pipelineVisibilityService.addVisibility(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            visibilityList = visibilityList
        )
        return Result(true)
    }

    override fun deleteVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        scopeIds: List<String>
    ): Result<Boolean> {
        pipelineVisibilityService.deleteVisibility(
            projectId = projectId,
            pipelineId = pipelineId,
            scopeIds = scopeIds
        )
        return Result(true)
    }

    override fun listVisibility(
        userId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Result<SQLPage<PipelineVisibility>> {
        return Result(
            pipelineVisibilityService.listVisibility(
                projectId = projectId,
                pipelineId = pipelineId,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                keyword = keyword
            )
        )
    }
}
