package com.tencent.devops.stream.resources.service

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.project.pojo.app.AppProjectVO
import com.tencent.devops.stream.api.service.ServiceGitForAppResource
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import com.tencent.devops.stream.service.StreamAppService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGitForAppResourceImpl @Autowired constructor(
    val streamAppService: StreamAppService
) : ServiceGitForAppResource {
    override fun getGitCIProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        searchName: String?
    ): Result<Pagination<AppProjectVO>> {
        return Result(streamAppService.getGitCIProjectList(userId, page, pageSize, searchName))
    }

    override fun getGitCIPipeline(projectId: String, pipelineId: String): Result<StreamGitProjectPipeline?> {
        return Result(streamAppService.getGitCIPipeline(projectId, pipelineId))
    }

    override fun getGitCIPipelines(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        search: String?
    ): Result<Pagination<StreamGitProjectPipeline>> {
        return Result(streamAppService.getGitCIPipelines(projectId, page, pageSize, sortType, search))
    }
}
