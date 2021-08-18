package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.GitCIDetailResource
import com.tencent.devops.gitci.api.GitCIHistoryResource
import com.tencent.devops.gitci.api.GitCIPipelineResource
import com.tencent.devops.gitci.api.service.ServiceStreamTriggerResource
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.gitci.pojo.GitCIModelDetail
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.StreamTriggerBuildReq
import com.tencent.devops.openapi.api.apigw.v3.ApigwStreamResourceV3
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStreamResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwStreamResourceV3 {

    override fun triggerStartup(
        appCode: String?,
        apigwType: String?,
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

    override fun getStreamProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: String
    ): Result<String> {
        val r1 = Regex("[0-9]+")
        return if (r1.matches(gitProjectId)) {
            Result("git_$gitProjectId")
        } else {
            Result(gitProjectId)
        }
    }

    override fun getPipelineList(
        appCode: String?,
        apigwType: String?,
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

    override fun getPipeline(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: Long,
        pipelineId: String
    ): Result<GitProjectPipeline?> {
        return client.get(GitCIPipelineResource::class).getPipeline(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        )
    }

    override fun enablePipeline(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        enabled: Boolean
    ): Result<Boolean> {
        return client.get(GitCIPipelineResource::class).enablePipeline(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            enabled = enabled
        )
    }

    override fun listPipelineNames(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: Long
    ): Result<List<GitProjectPipeline>> {
        return client.get(GitCIPipelineResource::class).listPipelineNames(
            userId = userId,
            gitProjectId = gitProjectId
        )
    }

    override fun getLatestBuildDetail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: Long,
        pipelineId: String?,
        buildId: String?
    ): Result<GitCIModelDetail?> {
        return client.get(GitCIDetailResource::class).getLatestBuildDetail(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    override fun getHistoryBuildList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?,
        branch: String?,
        sourceGitProjectId: Long?,
        triggerUser: String?,
        pipelineId: String?
    ): Result<Page<GitCIBuildHistory>> {
        return client.get(GitCIHistoryResource::class).getHistoryBuildList(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            triggerUser = triggerUser,
            branch = branch,
            sourceGitProjectId = sourceGitProjectId,
            page = page,
            pageSize = pageSize
        )
    }
}
