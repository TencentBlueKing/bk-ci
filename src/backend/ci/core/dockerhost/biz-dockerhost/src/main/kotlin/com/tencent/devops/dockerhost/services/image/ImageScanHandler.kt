package com.tencent.devops.dockerhost.services.image

import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import org.springframework.stereotype.Service

@Service
class ImageScanHandler(
    private val dockerHostImageScanService: DockerHostImageScanService
) : Handler<ImageHandlerContext>() {
    override fun handlerRequest(handlerContext: ImageHandlerContext) {
        with(handlerContext) {
            if (scanFlag) {
                dockerHostImageScanService.scanningDocker(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userName = userName,
                    imageTagSet = imageTagSet,
                    dockerClient = dockerClient
                )
            }

            nextHandler?.handlerRequest(this)
        }
    }
}
