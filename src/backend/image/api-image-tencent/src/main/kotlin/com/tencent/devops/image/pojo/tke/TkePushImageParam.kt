package com.tencent.devops.image.pojo.tke

data class TkePushImageParam(
    val userId: String,
    val srcImageName: String,
    val srcImageTag: String,
    val repoAddress: String,
    val userName: String,
    val password: String,
    val targetImageName: String,
    val targetImageTag: String,
    val projectId: String,
    val buildId: String,
    val pipelineId: String,
    val taskId: String,
    val containerId: String,
    val codeUrl: String?,
    val executeCount: Int?,
    val cmdbId: Int,
    val verifyOa: Boolean
) {
    fun outStr(): String {
        return StringBuffer().append("pull image from jfrog, projectId: $projectId, ")
            .append("pipelineId: $pipelineId, ")
            .append("buildId: $buildId, ")
            .append("repoAddress: $repoAddress, ")
            .append("userName: $userName, ")
            .append("srcImageName: $srcImageName, ")
            .append("srcImageTag: $srcImageTag, ")
            .append("targetImageName: $targetImageName, ")
            .append("targetImageTag: $targetImageTag")
            .append("taskId: $taskId")
            .append("codeUrl: $codeUrl")
            .append("executeCount: $executeCount")
            .append("cmdbId: $cmdbId")
            .append("verifyOa: $verifyOa")
            .toString()
    }
}
