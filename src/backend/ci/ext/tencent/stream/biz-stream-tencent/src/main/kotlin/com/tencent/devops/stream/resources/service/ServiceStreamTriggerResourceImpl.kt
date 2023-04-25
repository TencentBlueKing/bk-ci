package com.tencent.devops.stream.resources.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.stream.api.service.ServiceStreamTriggerResource
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.ManualTriggerInfo
import com.tencent.devops.stream.pojo.OpenapiTriggerReq
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.pojo.TriggerBuildResult
import com.tencent.devops.stream.pojo.openapi.StreamTriggerBuildReq
import com.tencent.devops.stream.pojo.openapi.StreamYamlCheck
import com.tencent.devops.stream.service.StreamGitTokenService
import com.tencent.devops.stream.service.StreamPipelineService
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.service.StreamYamlService
import com.tencent.devops.stream.trigger.ManualTriggerService
import com.tencent.devops.stream.trigger.OpenApiTriggerService
import com.tencent.devops.stream.util.GitCommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceStreamTriggerResourceImpl @Autowired constructor(
    private val openApiTriggerService: OpenApiTriggerService,
    private val permissionService: StreamPermissionService,
    private val streamScmService: StreamScmService,
    private val streamGitTokenService: StreamGitTokenService,
    private val manualTriggerService: ManualTriggerService,
    private val streamYamlService: StreamYamlService,
    private val streamPipelineService: StreamPipelineService
) : ServiceStreamTriggerResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceStreamTriggerResourceImpl::class.java)
    }

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
        return Result(openApiTriggerService.triggerBuild(userId, pipelineId, new))
    }

    override fun getManualTriggerInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        commitId: String?
    ): Result<ManualTriggerInfo> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(userId, projectId, gitProjectId)
        try {
            return Result(
                manualTriggerService.getManualTriggerInfo(
                    userId = userId,
                    pipelineId = pipelineId,
                    projectId = projectId,
                    branchName = branchName,
                    commitId = commitId
                )
            )
        } catch (e: ErrorCodeException) {
            return Result(
                status = e.statusCode,
                message = e.defaultMessage
            )
        } catch (e: Exception) {
            return Result(
                status = ErrorCodeEnum.MANUAL_TRIGGER_YAML_INVALID.errorCode,
                message = "Invalid yaml: ${e.message}"
            )
        }
    }

    override fun openapiTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        triggerBuildReq: OpenapiTriggerReq
    ): Result<TriggerBuildResult> {
        logger.info("STREAM_TRIGGER_SERVICE|openapiTrigger|$userId|$projectId|$pipelineId|$triggerBuildReq")
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamAndOAuthAndEnable(userId, triggerBuildReq.projectId, gitProjectId)
        return with(triggerBuildReq) {
            val openapiInput = inputs?.toMutableMap()
            val checkPipelineTrigger = openapiInput?.remove("ThisIsSubPipelineExecStream", "")
            Result(
                openApiTriggerService.triggerBuild(
                    userId, pipelineId,
                    TriggerBuildReq(
                        projectId = projectId,
                        branch = branch,
                        customCommitMsg = customCommitMsg,
                        yaml = getYamlContentWithPath(
                            gitProjectId = gitProjectId,
                            branch = branch,
                            path = path
                        ),
                        description = null,
                        commitId = commitId,
                        payload = null,
                        eventType = null,
                        inputs = ManualTriggerService.parseInputs(openapiInput),
                        checkPipelineTrigger = checkPipelineTrigger ?: false
                    )
                )
            )
        }
    }

    override fun checkYaml(userId: String, yamlCheck: StreamYamlCheck): Result<String> {
        logger.info("STREAM_TRIGGER_SERVICE|checkYaml|$userId|$yamlCheck")
        return streamYamlService.checkYaml(
            originYaml = yamlCheck.originYaml,
            templateType = yamlCheck.templateType?.let { TemplateType.valueOf(it) },
            isCiFile = yamlCheck.checkCiFile
        )
    }

    override fun nameToPipelineId(
        userId: String,
        projectId: String,
        yamlPath: String
    ): Result<StreamGitProjectPipeline> {
        logger.info("STREAM_TRIGGER_SERVICE|nameToPipelineId|$userId|$projectId|$yamlPath")
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        permissionService.checkStreamAndOAuthAndEnable(userId, projectId, gitProjectId, AuthPermission.VIEW)
        return Result(streamPipelineService.getPipelineInfoByYamlPath(gitProjectId, yamlPath))
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
