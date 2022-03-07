package com.tencent.devops.dockerhost.services.container

import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsVO
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.dockerhost.pojo.DockerRunResponse
import com.tencent.devops.dockerhost.services.HandlerContext

data class ContainerHandlerContext(
    val originImageName: String,
    val agentId: String?,
    val secretKey: String?,
    val registryUser: String?,
    val registryPwd: String?,
    val imageType: String?,
    val containerHashId: String? = null,
    val customBuildEnv: Map<String, String>? = null,
    val buildType: BuildType = BuildType.DOCKER,
    val qpcUniquePath: String? = null,
    val dockerResource: DockerResourceOptionsVO? = null,
    val dockerRunParam: DockerRunParam? = null,
    val specialProjectList: String? = null,
    var formatImageName: String? = "",
    var containerId: String? = "",
    var dockerRunResponse: DockerRunResponse? = null,
    override val projectId: String,
    override val pipelineId: String,
    override val buildId: String,
    override val vmSeqId: Int,
    override val poolNo: Int,
    override val userName: String,
    override val pipelineTaskId: String? = null
) : HandlerContext(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    vmSeqId = vmSeqId,
    poolNo = poolNo,
    userName = userName,
    pipelineTaskId = pipelineTaskId
)
