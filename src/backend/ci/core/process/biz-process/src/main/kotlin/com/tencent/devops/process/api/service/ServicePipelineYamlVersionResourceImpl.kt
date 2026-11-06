package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlBranchActionReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlDeployReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlPullRequestReq
import com.tencent.devops.process.yaml.resource.IPipelineYamlResourceService
import com.tencent.devops.process.yaml.resource.PTemplateYamlResourceService
import com.tencent.devops.process.yaml.resource.PipelineYamlResourceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@RestResource
class ServicePipelineYamlVersionResourceImpl @Autowired constructor(
    private val pipelineYamlResourceService: PipelineYamlResourceService,
    @Lazy private val pTemplateYamlResourceService: PTemplateYamlResourceService
) : ServicePipelineYamlVersionResource {

    override fun createYamlPipeline(
        userId: String,
        projectId: String,
        isTemplate: Boolean,
        request: PipelineYamlDeployReq
    ): Result<DeployPipelineResult> {
        return Result(
            getService(isTemplate).createYamlPipeline(
                userId = userId,
                projectId = projectId,
                request = request
            )
        )
    }

    override fun updateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean,
        request: PipelineYamlDeployReq
    ): Result<DeployPipelineResult> {
        return Result(
            getService(isTemplate).updateYamlPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                request = request
            )
        )
    }

    override fun updateBranchAction(
        userId: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean,
        request: PipelineYamlBranchActionReq
    ): Result<Boolean> {
        getService(isTemplate).updateBranchAction(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            branchName = request.branchName,
            branchVersionAction = request.branchVersionAction
        )
        return Result(true)
    }

    override fun deletePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean
    ): Result<Boolean> {
        getService(isTemplate).deletePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
        return Result(true)
    }

    override fun getPipelineName(
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean
    ): Result<String?> {
        return Result(
            getService(isTemplate).getPipelineName(
                projectId = projectId,
                pipelineId = pipelineId
            )
        )
    }

    override fun existsReleaseVersion(
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean
    ): Result<Boolean> {
        return Result(
            getService(isTemplate).existsReleaseVersion(
                projectId = projectId,
                pipelineId = pipelineId
            )
        )
    }

    override fun completePullRequest(
        userId: String,
        projectId: String,
        pipelineId: String,
        isTemplate: Boolean,
        request: PipelineYamlPullRequestReq
    ): Result<Boolean> {
        getService(isTemplate).completePullRequest(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pullRequestId = request.pullRequestId,
            pullRequestUrl = request.pullRequestUrl,
            pullRequestNumber = request.pullRequestNumber,
            merged = request.merged,
            instanceErrorInfo = request.instanceErrorInfo
        )
        return Result(true)
    }

    private fun getService(isTemplate: Boolean): IPipelineYamlResourceService {
        return if (isTemplate) {
            pTemplateYamlResourceService
        } else {
            pipelineYamlResourceService
        }
    }
}
