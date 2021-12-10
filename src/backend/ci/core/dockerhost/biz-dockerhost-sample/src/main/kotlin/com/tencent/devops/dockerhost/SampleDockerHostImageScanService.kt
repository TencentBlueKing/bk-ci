package com.tencent.devops.dockerhost

import com.github.dockerjava.api.DockerClient
import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import com.tencent.devops.dockerhost.services.image.ImageHandlerContext
import org.springframework.stereotype.Component

@Component
class SampleDockerHostImageScanService : DockerHostImageScanService {
    override fun scanningDocker(
        imageHandlerContext: ImageHandlerContext,
        dockerClient: DockerClient
    ): String {
        // do something before push images
        return ""
    }
}
