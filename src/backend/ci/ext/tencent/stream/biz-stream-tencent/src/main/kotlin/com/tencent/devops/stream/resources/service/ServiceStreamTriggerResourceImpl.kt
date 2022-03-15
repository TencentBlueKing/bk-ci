package com.tencent.devops.stream.resources.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.service.ServiceStreamTriggerResource
import com.tencent.devops.stream.permission.GitCIV2PermissionService
import com.tencent.devops.stream.pojo.StreamTriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.trigger.ManualTriggerService
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamGitTokenService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStreamTriggerResourceImpl @Autowired constructor(
    private val manualTriggerService: ManualTriggerService,
    private val permissionService: GitCIV2PermissionService,
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
        permissionService.checkGitCIAndOAuthAndEnable(userId, projectId, gitProjectId)
        val new = with(streamTriggerBuildReq) {
            com.tencent.devops.stream.pojo.TriggerBuildReq(
                gitProjectId = gitProjectId,
                name = null,
                url = null,
                homepage = null,
                gitHttpUrl = null,
                gitSshUrl = null,
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
                scmType = scmType,
                eventType = eventType,
                objectKind = objectKind
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
