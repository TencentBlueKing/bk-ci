package com.tencent.devops.dockerhost.services.image

import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.services.Handler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ImageDeleteHandler(
    dockerHostConfig: DockerHostConfig,
    dockerHostBuildApi: DockerHostBuildResourceApi
) : Handler<ImageHandlerContext>(dockerHostConfig, dockerHostBuildApi) {
    override fun handlerRequest(handlerContext: ImageHandlerContext) {
        with(handlerContext) {
            // 扫描模式下不做镜像删除
            if (scanFlag) {
                return
            }

            imageTagSet.parallelStream().forEach {
                logger.info("[$buildId]|[$vmSeqId] Push image success, now remove local image, " +
                        "image name and tag: $it")
                try {
                    dockerClient.removeImageCmd(it).exec()
                    logger.info("[$buildId]|[$vmSeqId] Remove local image success")
                } catch (e: Throwable) {
                    logger.error("[$buildId]|[$vmSeqId] Docker rmi failed, msg: ${e.message}")
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImageDeleteHandler::class.java)
    }
}
