package com.tencent.devops.dockerhost.service

import com.github.dockerjava.api.DockerClient
import com.tencent.devops.common.api.util.script.ShellUtil
import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.Executors

@Component
class TXDockerHostImageScanService : DockerHostImageScanService {

    @Value("\${dockerCli.dockerSavedPath:/data/dockersavedimages}")
    var dockerSavedPath: String? = "/data/dockersavedimages"

    override fun scanningDocker(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        imageTagSet: MutableSet<String>,
        dockerClient: DockerClient
    ) {
        Executors.newFixedThreadPool(10).execute {
            try {
                imageTagSet.stream().forEach {
                    val inputStream = dockerClient.saveImageCmd(it.substringBeforeLast(":"))
                        .withTag(it.substringAfterLast(":"))
                        .exec()
                    val imageSavedPath = "$dockerSavedPath/${it.substringBeforeLast(":")}.tar"
                    val targetSavedImagesFile = File(imageSavedPath)
                    FileUtils.copyInputStreamToFile(inputStream, targetSavedImagesFile)

                    val script = "dockerscan -t $imageSavedPath -p $pipelineId -u $it -i dev " +
                            "-T $projectId -b $buildId -n sawyersong"
                    val scanResult = ShellUtil.executeEnhance(script)
                    logger.info("[$buildId]|[$vmSeqId] scan docker $it result: $scanResult")
                }
            } catch (e: Exception) {
                logger.error("[$buildId]|[$vmSeqId] scan docker error.", e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TXDockerHostImageScanService::class.java)
    }
}
