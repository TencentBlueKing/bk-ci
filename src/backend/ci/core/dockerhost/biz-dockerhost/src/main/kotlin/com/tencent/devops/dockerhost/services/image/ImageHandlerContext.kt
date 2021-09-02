package com.tencent.devops.dockerhost.services.image

import com.github.dockerjava.api.DockerClient
import com.tencent.devops.dockerhost.pojo.DockerBuildParam

data class ImageHandlerContext(
    val outer: Boolean, // 是否为外部请求创建镜像
    val scanFlag: Boolean, // 镜像扫描开关
    val dockerClient: DockerClient,
    val pipelineTaskId: String?,
    val dockerBuildParam: DockerBuildParam,
    var imageTagSet: MutableSet<String> = mutableSetOf(),
    override val projectId: String,
    override val pipelineId: String,
    override val buildId: String,
    override val vmSeqId: String
) : HandlerContext(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    vmSeqId = vmSeqId
)
