package com.tencent.devops.dockerhost.services

import com.github.dockerjava.api.DockerClient

interface DockerHostImageScanService {
    fun scanningDocker(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userName: String,
        imageTagSet: MutableSet<String>,
        dockerClient: DockerClient
    ): String
}
