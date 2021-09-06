package com.tencent.devops.stream.resources.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.service.ServiceStreamTriggerResource
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.pojo.StreamTriggerBuildReq
import com.tencent.devops.stream.trigger.GitCITriggerService
import com.tencent.devops.stream.utils.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStreamTriggerResourceImpl @Autowired constructor(
    private val gitCITriggerService: GitCITriggerService,
    private val permissionService: GitCIV2PermissionService
) : ServiceStreamTriggerResource {

    override fun triggerStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<Boolean> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        val new = with(streamTriggerBuildReq) {
            com.tencent.devops.stream.pojo.TriggerBuildReq(
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
        return Result(gitCITriggerService.triggerBuild(userId, pipelineId, new))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
