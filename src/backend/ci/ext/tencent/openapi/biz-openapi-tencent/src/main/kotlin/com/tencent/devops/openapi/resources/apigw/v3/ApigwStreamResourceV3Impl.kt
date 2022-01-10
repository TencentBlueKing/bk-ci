package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.GitCIDetailResource
import com.tencent.devops.stream.api.GitCIHistoryResource
import com.tencent.devops.stream.api.GitCIPipelineResource
import com.tencent.devops.stream.api.service.ServiceGitBasicSettingResource
import com.tencent.devops.stream.api.service.ServiceStreamTriggerResource
import com.tencent.devops.stream.pojo.GitCIBuildHistory
import com.tencent.devops.stream.pojo.GitCIModelDetail
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.StreamTriggerBuildReq
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.pojo.v2.GitCIUpdateSetting
import com.tencent.devops.openapi.api.apigw.v3.ApigwStreamResourceV3
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.stream.api.service.ServiceGitCIProjectResource
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.enums.GitCIProjectType
import com.tencent.devops.stream.pojo.v2.GitUserValidateRequest
import com.tencent.devops.stream.pojo.v2.GitUserValidateResult
import com.tencent.devops.stream.pojo.v2.project.ProjectCIInfo
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
    ): Result<TriggerBuildResult> {
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
        pipelineId: String,
        withHistory: Boolean?
    ): Result<GitProjectPipeline?> {
        return client.get(GitCIPipelineResource::class).getPipeline(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            withHistory = withHistory
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

    override fun enableGitCI(
        appCode: String?,
        apigwType: String?,
        userId: String,
        enabled: Boolean,
        projectInfo: GitCIProjectInfo
    ): Result<Boolean> {
        return client.get(ServiceGitBasicSettingResource::class).enableGitCI(
            userId = userId,
            enabled = enabled,
            projectInfo = projectInfo
        )
    }

    override fun getGitCIConf(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: String
    ): Result<GitCIBasicSetting?> {
        return client.get(ServiceGitBasicSettingResource::class).getGitCIConf(
            userId = userId,
            projectId = gitProjectId
        )
    }

    override fun saveGitCIConf(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: String,
        gitCIUpdateSetting: GitCIUpdateSetting
    ): Result<Boolean> {
        return client.get(ServiceGitBasicSettingResource::class).saveGitCIConf(
            userId = userId,
            projectId = gitProjectId,
            gitCIUpdateSetting = gitCIUpdateSetting
        )
    }

    override fun validateGitProject(userId: String, request: GitUserValidateRequest): Result<GitUserValidateResult?> {
        return client.get(ServiceGitBasicSettingResource::class).validateGitProject(
            userId = userId,
            request = request
        )
    }

    override fun updateEnableUser(
        userId: String,
        gitProjectId: String,
        authUserId: String
    ): Result<Boolean> {
        return client.get(ServiceGitBasicSettingResource::class).updateEnableUser(
            userId = userId,
            projectId = "git_$gitProjectId",
            authUserId = authUserId
        )
    }

    override fun getProjects(
        userId: String,
        type: GitCIProjectType?,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?
    ): Result<List<ProjectCIInfo>> {
        return client.get(ServiceGitCIProjectResource::class).getProjects(
            userId = userId,
            type = type,
            search = search,
            page = page,
            pageSize = pageSize,
            orderBy = orderBy,
            sort = sort
        )
    }
}
