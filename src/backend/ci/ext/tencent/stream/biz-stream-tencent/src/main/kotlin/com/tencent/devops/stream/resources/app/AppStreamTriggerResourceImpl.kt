package com.tencent.devops.stream.resources.app

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.app.AppStreamTriggerResource
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.pojo.AppTriggerBuildResult
import com.tencent.devops.stream.pojo.V1TriggerBuildReq
import com.tencent.devops.stream.pojo.V2AppTriggerBuildReq
import com.tencent.devops.stream.trigger.ManualTriggerService
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamPipelineService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class AppStreamTriggerResourceImpl @Autowired constructor(
    private val manualTriggerService: ManualTriggerService,
    private val permissionService: GitCIV2PermissionService,
    private val streamPipelineService: StreamPipelineService
) : AppStreamTriggerResource {
    override fun triggerStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerBuildReq: V2AppTriggerBuildReq
    ): Result<AppTriggerBuildResult> {
        checkParam(userId)
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        val yaml = triggerBuildReq.yaml ?: streamPipelineService.getYamlByPipeline(
            gitProjectId,
            pipelineId,
            triggerBuildReq.commitId?.ifBlank { triggerBuildReq.branch } ?: triggerBuildReq.branch
        )
        if (yaml.isNullOrBlank()) {
            throw CustomException(
                Response.Status.NOT_FOUND,
                "Not found stream yaml"
            )
        }

        val new = with(triggerBuildReq) {
            V1TriggerBuildReq(
                gitProjectId = gitProjectId,
                name = null,
                url = null,
                homepage = null,
                gitHttpUrl = null,
                gitSshUrl = null,
                branch = branch,
                customCommitMsg = customCommitMsg,
                yaml = yaml,
                description = description,
                commitId = commitId
            )
        }
        return Result(AppTriggerBuildResult(manualTriggerService.triggerBuild(userId, pipelineId, new).buildId))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
