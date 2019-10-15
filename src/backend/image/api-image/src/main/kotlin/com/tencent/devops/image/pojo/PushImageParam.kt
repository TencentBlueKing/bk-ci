package com.tencent.devops.image.pojo

import io.swagger.annotations.ApiModelProperty

data class PushImageParam(
    @ApiModelProperty("用户ID", required = true)
    val userId: String,
    @ApiModelProperty("源镜像名称", required = true)
    val srcImageName: String,
    @ApiModelProperty("源镜像tag", required = true)
    val srcImageTag: String,
    @ApiModelProperty("镜像仓库地址", required = true)
    val repoAddress: String,
    @ApiModelProperty("目标镜像命名空间", required = true)
    val namespace: String,
    @ApiModelProperty("凭证ID", required = false)
    val ticketId: String?,
    @ApiModelProperty("目标镜像名称", required = true)
    val targetImageName: String,
    @ApiModelProperty("目标镜像tag", required = true)
    val targetImageTag: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("插件执行次数", required = false)
    val executeCount: Int?
) {
    fun outStr(): String {
        return StringBuffer().append("pull image from jfrog, projectId: $projectId, ")
            .append("pipelineId: $pipelineId, ")
            .append("buildId: $buildId, ")
            .append("repoAddress: $repoAddress, ")
            .append("namespace: $namespace, ")
            .append("ticketId: $ticketId, ")
            .append("srcImageName: $srcImageName, ")
            .append("srcImageTag: $srcImageTag, ")
            .append("targetImageName: $targetImageName, ")
            .append("targetImageTag: $targetImageTag, ")
            .append("executeCount: $executeCount")
            .toString()
    }
}
