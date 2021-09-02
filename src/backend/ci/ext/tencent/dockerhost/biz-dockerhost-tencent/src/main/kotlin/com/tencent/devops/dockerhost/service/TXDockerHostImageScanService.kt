package com.tencent.devops.dockerhost.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.common.api.util.script.ShellUtil
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.services.DockerHostImageScanService
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

@Component
class TXDockerHostImageScanService(
    private val dockerHostConfig: DockerHostConfig
) : DockerHostImageScanService {

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
            lateinit var longDockerClient: DockerClient
            try {
                val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerConfig(dockerHostConfig.dockerConfig)
                    .withApiVersion(dockerHostConfig.apiVersion)
                    .build()

                val longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
                    .dockerHost(config.dockerHost)
                    .sslConfig(config.sslConfig)
                    .connectTimeout(5000)
                    .readTimeout(600000)
                    .build()

                longDockerClient = DockerClientBuilder.getInstance(config).withDockerHttpClient(longHttpClient).build()

                imageTagSet.stream().forEach {
                    val inputStream = longDockerClient.saveImageCmd(it.substringBeforeLast(":"))
                        .withTag(it.substringAfterLast(":"))
                        .exec()
                    val imageSavedPath = "$dockerSavedPath/${it.substringBeforeLast(":")}.tar"
                    val targetSavedImagesFile = File(imageSavedPath)
                    FileUtils.copyInputStreamToFile(inputStream, targetSavedImagesFile)

                    val script = "dockerscan -t $imageSavedPath -p $pipelineId -u $it -i dev " +
                            "-T $projectId -b $buildId -n sawyersong"
                    val scanResult = ShellUtil.executeEnhance(script)
                    logger.info("[$buildId]|[$vmSeqId] scan docker $it result: $scanResult")

                    logger.info("[$buildId]|[$vmSeqId] scan image success, now remove local image, image name and tag: $it")
                    try {
                        longDockerClient.removeImageCmd(it).exec()
                        logger.info("[$buildId]|[$vmSeqId] Remove local image success")
                    } catch (e: Throwable) {
                        logger.error("[$buildId]|[$vmSeqId] Docker remove image failed, msg: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                logger.error("[$buildId]|[$vmSeqId] scan docker error.", e)
            } finally {
                try {
                    longDockerClient.close()
                } catch (e: IOException) {
                    logger.error("[$buildId]|[$vmSeqId] Long docker client close exception: ${e.message}")
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TXDockerHostImageScanService::class.java)
    }
}
