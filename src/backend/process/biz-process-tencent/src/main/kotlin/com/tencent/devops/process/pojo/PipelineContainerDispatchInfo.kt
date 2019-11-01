package com.tencent.devops.process.pojo

data class PipelineContainerDispatchInfo(
    val containerId: String,
    val pipelineId: String,
    val pipelineVersion: Int,
    val projectId: String,
    val dispatchBuildType: String,
    val dispatchValue: String,
    val dispatchImageType: String?,
    val dispatchCredentialId: String?,
    val dispatchWorkspace: String?,
    val dispatchAgentType: String?
) {
    override fun toString(): String {
        return "PipelineContainerDispatchInfo(containerId='$containerId', pipelineId='$pipelineId', pipelineVersion=$pipelineVersion, projectId='$projectId', dispatchBuildType='$dispatchBuildType', dispatchValue='$dispatchValue', dispatchImageType=$dispatchImageType, dispatchCredentialId=$dispatchCredentialId, dispatchWorkspace=$dispatchWorkspace, dispatchAgentType=$dispatchAgentType)"
    }
}