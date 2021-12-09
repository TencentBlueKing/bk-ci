package com.tencent.devops.dockerhost

import com.github.dockerjava.api.DockerClient
import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import org.springframework.stereotype.Component

@Component
class SampleDockerHostImageScanService : DockerHostImageScanService {
    override fun scanningDocker(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userName: String,
        imageId: String,
        dockerClient: DockerClient
    ): String {
        // do something before push images
        return ""
    }
}
