package com.tencent.devops.dockerhost.services

import com.github.dockerjava.api.DockerClient
import com.tencent.devops.dockerhost.services.image.ImageHandlerContext

interface DockerHostImageScanService {
    fun scanningDocker(
        imageHandlerContext: ImageHandlerContext,
        dockerClient: DockerClient
    ): String
}
