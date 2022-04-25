package com.tencent.devops.stream.resources.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.service.ServiceStreamTriggerResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.openapi.StreamTriggerBuildReq
import com.tencent.devops.stream.service.StreamGitTokenService
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.trigger.ManualTriggerService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStreamTriggerResourceImpl @Autowired constructor(
    private val manualTriggerService: ManualTriggerService,
    private val permissionService: StreamPermissionService,
    private val streamScmService: StreamScmService,
    private val streamGitTokenService: StreamGitTokenService
) : ServiceStreamTriggerResource {

    override fun triggerStartup(
        userId: String,
        projectId: String,
        pipelineId: String,
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<TriggerBuildResult> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(userId, projectId, gitProjectId)
        val new = with(streamTriggerBuildReq) {
            TriggerBuildReq(
                projectId = projectId,
                branch = branch ?: "",
                customCommitMsg = customCommitMsg,
                yaml = yaml ?: getYamlContentWithPath(
                    gitProjectId = gitProjectId,
                    branch = branch,
                    path = path
                ),
                description = description,
                commitId = commitId,
                payload = payload,
                eventType = eventType
            )
        }
        return Result(manualTriggerService.triggerBuild(userId, pipelineId, new))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    // 根据分支 +路径获取yaml文件内容
    private fun getYamlContentWithPath(gitProjectId: Long, branch: String?, path: String?): String? {

        return if (branch.isNullOrEmpty() || path.isNullOrEmpty()) {
            null
        } else {
            streamScmService.getYamlFromGit(
                token = streamGitTokenService.getToken(gitProjectId),
                gitProjectId = gitProjectId.toString(),
                fileName = path,
                ref = branch,
                useAccessToken = true
            )
        }
    }
}
