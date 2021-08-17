package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.GitCIPipelineResource
import com.tencent.devops.gitci.api.service.ServiceStreamTriggerResource
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.StreamTriggerBuildReq
import com.tencent.devops.openapi.api.apigw.v3.ApigwStreamResourceV3
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStreamResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwStreamResourceV3 {

    override fun triggerStartup(
        userId: String,
        gitProjectId: String,
        pipelineId: String,
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<Boolean> {
        return client.get(ServiceStreamTriggerResource::class).triggerStartup(
            userId = userId,
            projectId = "git_$gitProjectId",
            pipelineId = pipelineId,
            streamTriggerBuildReq = streamTriggerBuildReq
        )
    }

    override fun getStreamProject(userId: String, gitProjectId: String): Result<String> {
        val r1 = Regex("[0-9]+")
        return if (r1.matches(gitProjectId)) {
            Result("git_$gitProjectId")
        } else {
            Result(gitProjectId)
        }
    }

    override fun getPipelineList(
        userId: String,
        gitProjectId: Long,
        keyword: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<GitProjectPipeline>> {
        return client.get(GitCIPipelineResource::class).getPipelineList(
            userId = userId,
            gitProjectId = gitProjectId,
            keyword = keyword,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getPipeline(userId: String, gitProjectId: Long, pipelineId: String): Result<GitProjectPipeline?> {
        TODO("Not yet implemented")
    }

    override fun enablePipeline(userId: String, gitProjectId: Long, pipelineId: String, enabled: Boolean): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun listPipelineNames(userId: String, gitProjectId: Long): Result<List<GitProjectPipeline>> {
        TODO("Not yet implemented")
    }
}
