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
import java.security.MessageDigest
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
        userName: String,
        imageId: String,
        dockerClient: DockerClient
    ): String {
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

            // 取任意一个tag扫描就可以
            val imageTag = imageTagSet.first()
            /*val inputStream = longDockerClient.saveImageCmd(imageTag.substringBeforeLast(":"))
                .withTag(imageTag.substringAfterLast(":"))
                .exec()

            val uniqueImageCode = toHexStr(MessageDigest.getInstance("SHA-1")
                .digest(imageTag.toByteArray()))
            val imageSavedPath = "$dockerSavedPath/$uniqueImageCode.tar"
            val targetSavedImagesFile = File(imageSavedPath)
            FileUtils.copyInputStreamToFile(inputStream, targetSavedImagesFile)*/

            try {
                logger.info("[$buildId]|[$vmSeqId] start scan dockerimage, imageId: $imageId")
                val script = "dockerscan -t $imageId -p $pipelineId -u $imageTag -i dev " +
                        "-T $projectId -b $buildId -n $userName"
                val scanResult = ShellUtil.executeEnhance(script)
                logger.info("[$buildId]|[$vmSeqId] scan docker $imageTag result: $scanResult")

                logger.info("[$buildId]|[$vmSeqId] scan image success, now remove local image, " +
                        "image name and tag: $imageTag")

                return scanResult
            } catch (e: Throwable) {
                logger.error("[$buildId]|[$vmSeqId] Docker image scan failed, msg: ${e.message}")
            } finally {
                // longDockerClient.removeImageCmd(imageTag).exec()
                // logger.info("[$buildId]|[$vmSeqId] Remove local image success")
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

        return ""
    }

    fun toHexStr(byteArray: ByteArray) =
        with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) append("0").append(hexStr)
                else append(hexStr)
            }
            toString()
        }

    companion object {
        private val logger = LoggerFactory.getLogger(TXDockerHostImageScanService::class.java)
    }
}
