package com.tencent.devops.process.yaml

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.actions.GitActionCommon
import com.tencent.devops.process.yaml.common.Constansts
import org.springframework.stereotype.Service

@Service
class PipelineYamlCommonService(
    private val pipelineYamlService: PipelineYamlService
) {

    fun checkPushParam(
        projectId: String,
        pipelineId: String,
        content: String,
        repoHashId: String,
        filePath: String,
        targetAction: CodeTargetAction,
        versionName: String?,
        targetBranch: String?
    ) {
        if (content.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_YAML_CONTENT_IS_EMPTY,
                params = arrayOf(repoHashId)
            )
        }
        if (filePath.startsWith(Constansts.ciFileDirectoryName) &&
            !GitActionCommon.checkYamlPipelineFile(filePath.substringAfter(".ci/"))
        ) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_YAML_FILE_NAME_FORMAT
            )
        }
        if (
            (targetAction != CodeTargetAction.COMMIT_TO_MASTER &&
                targetAction != CodeTargetAction.COMMIT_TO_BRANCH) &&
            versionName.isNullOrBlank()
        ) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("versionName")
            )
        }
        if (targetAction == CodeTargetAction.COMMIT_TO_BRANCH && targetBranch.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("targetBranch")
            )
        }
        pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId, repoHashId = repoHashId, filePath = filePath
        )?.let {
            if (it.pipelineId != pipelineId) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_YAML_BOUND_PIPELINE,
                    params = arrayOf(filePath, it.pipelineId)
                )
            }
        }
        pipelineYamlService.getPipelineYamlInfo(projectId = projectId, pipelineId = pipelineId)?.let {
            if (it.repoHashId != repoHashId) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BOUND_REPO,
                    params = arrayOf(it.repoHashId)
                )
            }
            if (it.filePath != filePath) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BOUND_YAML,
                    params = arrayOf(it.filePath)
                )
            }
        }
    }
}
