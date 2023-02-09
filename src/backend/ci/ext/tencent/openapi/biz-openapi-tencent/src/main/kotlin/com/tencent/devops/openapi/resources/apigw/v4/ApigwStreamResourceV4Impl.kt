package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwStreamResourceV4
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.stream.api.service.ServiceGitBasicSettingResource
import com.tencent.devops.stream.api.service.ServiceGitCIProjectResource
import com.tencent.devops.stream.api.service.ServiceStreamTriggerResource
import com.tencent.devops.stream.api.service.v1.GitCIDetailResource
import com.tencent.devops.stream.api.service.v1.GitCIHistoryResource
import com.tencent.devops.stream.api.service.v1.GitCIPipelineResource
import com.tencent.devops.stream.pojo.ManualTriggerInfo
import com.tencent.devops.stream.pojo.OpenapiTriggerReq
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.openapi.GitCIBasicSetting
import com.tencent.devops.stream.pojo.openapi.GitCIProjectType
import com.tencent.devops.stream.pojo.openapi.GitCIUpdateSetting
import com.tencent.devops.stream.pojo.openapi.GitUserValidateRequest
import com.tencent.devops.stream.pojo.openapi.GitUserValidateResult
import com.tencent.devops.stream.pojo.openapi.ProjectCIInfo
import com.tencent.devops.stream.pojo.openapi.StreamTriggerBuildReq
import com.tencent.devops.stream.pojo.openapi.StreamYamlCheck
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitCIModelDetail
import com.tencent.devops.stream.v1.pojo.V1GitProjectPipeline
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwStreamResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwStreamResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwStreamResourceV4Impl::class.java)
        private const val MAX_PAGE_SIZE = 50
    }

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

    override fun getManualTriggerInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        commitId: String?
    ): Result<ManualTriggerInfo> {
        return client.get(ServiceStreamTriggerResource::class).getManualTriggerInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = branchName,
            commitId = commitId
        )
    }

    override fun openapiTrigger(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerBuildReq: OpenapiTriggerReq
    ): Result<TriggerBuildResult> {
        logger.info("STREAM_V4|openapiTrigger|$userId|$projectId|$pipelineId|$triggerBuildReq")
        return client.get(ServiceStreamTriggerResource::class).openapiTrigger(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            triggerBuildReq = triggerBuildReq
        )
    }

    override fun nameToPipelineId(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        yamlPath: String
    ): Result<StreamGitProjectPipeline> {
        logger.info("STREAM_V4|nameToPipelineId|$userId|$projectId|$yamlPath")
        return client.get(ServiceStreamTriggerResource::class).nameToPipelineId(
            userId = userId,
            projectId = projectId,
            yamlPath = yamlPath
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
    ): Result<Page<V1GitProjectPipeline>> {
        val realPageSize = if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
            MAX_PAGE_SIZE
        } else {
            pageSize
        }
        return client.get(GitCIPipelineResource::class).getPipelineList(
            userId = userId,
            gitProjectId = gitProjectId,
            keyword = keyword,
            page = page,
            pageSize = realPageSize
        )
    }

    override fun getPipeline(
        appCode: String?,
        apigwType: String?,
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        withHistory: Boolean?
    ): Result<V1GitProjectPipeline?> {
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
    ): Result<List<V1GitProjectPipeline>> {
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
    ): Result<V1GitCIModelDetail?> {
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
        startBeginTime: String?,
        endBeginTime: String?,
        page: Int?,
        pageSize: Int?,
        branch: String?,
        sourceGitProjectId: Long?,
        triggerUser: String?,
        pipelineId: String?
    ): Result<Page<V1GitCIBuildHistory>> {
        val realPageSize = if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
            MAX_PAGE_SIZE
        } else {
            pageSize
        }
        return client.get(GitCIHistoryResource::class).getHistoryBuildList(
            userId = userId,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            triggerUser = triggerUser,
            branch = branch,
            sourceGitProjectId = sourceGitProjectId,
            startBeginTime = startBeginTime,
            endBeginTime = endBeginTime,
            page = page,
            pageSize = realPageSize
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
            projectId = "git_$gitProjectId"
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
            projectId = "git_$gitProjectId",
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
        val realPageSize = if (pageSize != null && pageSize > MAX_PAGE_SIZE) {
            MAX_PAGE_SIZE
        } else {
            pageSize
        }
        return client.get(ServiceGitCIProjectResource::class).getProjects(
            userId = userId,
            type = type,
            search = search,
            page = page,
            pageSize = realPageSize,
            orderBy = orderBy,
            sort = sort
        )
    }

    override fun checkYaml(userId: String, gitProjectId: String?, yamlCheck: StreamYamlCheck): Result<String> {
        return client.get(ServiceStreamTriggerResource::class).checkYaml(userId, yamlCheck)
    }
}
