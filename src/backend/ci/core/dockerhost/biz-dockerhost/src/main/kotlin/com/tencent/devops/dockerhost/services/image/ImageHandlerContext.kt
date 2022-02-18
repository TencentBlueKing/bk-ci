package com.tencent.devops.dockerhost.services.image

import com.github.dockerjava.api.DockerClient
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.services.HandlerContext

data class ImageHandlerContext(
    val outer: Boolean, // 是否为外部请求创建镜像
    val scanFlag: Boolean, // 镜像扫描开关
    val dockerClient: DockerClient,
    val dockerBuildParam: DockerBuildParam,
    var result: String? = null,
    var imageTagSet: MutableSet<String> = mutableSetOf(),
    var imageId: String = "",
    override val projectId: String,
    override val pipelineId: String,
    override val buildId: String,
    override val vmSeqId: Int,
    override val poolNo: Int,
    override val userName: String,
    override val pipelineTaskId: String?
) : HandlerContext(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    vmSeqId = vmSeqId,
    poolNo = poolNo,
    userName = userName,
    pipelineTaskId = pipelineTaskId
)
