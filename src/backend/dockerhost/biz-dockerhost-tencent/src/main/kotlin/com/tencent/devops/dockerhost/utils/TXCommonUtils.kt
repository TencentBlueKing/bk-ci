package com.tencent.devops.dockerhost.utils

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.dockerhost.config.TXDockerHostConfig
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object TXCommonUtils {

    private val logger = LoggerFactory.getLogger(TXCommonUtils::class.java)

    private val dockerHubUrl = "https://index.docker.io/v1/"

    fun normalizeImageName(imageNameStr: String): String {
        val (url, name, tag) = parseImage(imageNameStr)
        return when (url) {
            dockerHubUrl -> "$name:$tag"
            else -> "$url/$name:$tag"
        }
    }

    fun parseImage(imageNameInput: String): Triple<String, String, String> {
        val imageNameStr = imageNameInput.removePrefix("http://").removePrefix("https://")
        val arry = imageNameStr.split(":")
        if (arry.size == 1) {
            val str = imageNameStr.split("/")
            return if (str.size == 1) {
                Triple(dockerHubUrl, imageNameStr, "latest")
            } else {
                Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
            }
        } else if (arry.size == 2) {
            val str = imageNameStr.split("/")
            when {
                str.size == 1 -> return Triple(dockerHubUrl, arry[0], arry[1])
                str.size >= 2 -> return if (str[0].contains(":")) {
                    Triple(str[0], imageNameStr.substringAfter(str[0] + "/"), "latest")
                } else {
                    if (str.last().contains(":")) {
                        val nameTag = str.last().split(":")
                        Triple(str[0], imageNameStr.substringAfter(str[0] + "/").substringBefore(":" + nameTag[1]), nameTag[1])
                    } else {
                        Triple(str[0], str.last(), "latest")
                    }
                }
                else -> {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
            }
        } else if (arry.size == 3) {
            val str = imageNameStr.split("/")
            if (str.size >= 2) {
                val tail = imageNameStr.removePrefix(str[0] + "/")
                val nameAndTag = tail.split(":")
                if (nameAndTag.size != 2) {
                    logger.error("image name invalid: $imageNameStr")
                    throw Exception("image name invalid.")
                }
                return Triple(str[0], nameAndTag[0], nameAndTag[1])
            } else {
                logger.error("image name invalid: $imageNameStr")
                throw Exception("image name invalid.")
            }
        } else {
            logger.error("image name invalid: $imageNameStr")
            throw Exception("image name invalid.")
        }
    }

    fun getDockerDefaultClient(dockerHostConfig: TXDockerHostConfig): DockerClient {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHostConfig.dockerHost)
                .withDockerConfig(dockerHostConfig.dockerConfig)
                .withApiVersion(dockerHostConfig.apiVersion)
                .withRegistryUrl(dockerHostConfig.registryUrl)
                .withRegistryUsername(dockerHostConfig.registryUsername)
                .withRegistryPassword(SecurityUtil.decrypt(dockerHostConfig.registryPassword!!))
                .build()

        return DockerClientBuilder.getInstance(config).build()
    }

    fun getAuthConfig(imageType: String?, dockerHostConfig: TXDockerHostConfig, imageName: String, registryUser: String?, registryPwd: String?): AuthConfig? {
        return if (imageType == ImageType.THIRD.type) {
            val (registryHost, _, _) = parseImage(imageName)
            logger.info("registry host: $registryHost")
            if (registryUser.isNullOrBlank()) {
                AuthConfig().withRegistryAddress(registryHost)
            } else {
                logger.info("registryUser: $registryUser, registryPwd: $registryPwd")
                AuthConfig()
                        .withUsername(registryUser)
                        .withPassword(registryPwd)
                        .withRegistryAddress(registryHost)
            }
        } else {
            AuthConfig()
                    .withUsername(dockerHostConfig.registryUsername)
                    .withPassword(SecurityUtil.decrypt(dockerHostConfig.registryPassword!!))
                    .withRegistryAddress(dockerHostConfig.registryUrl)
        }
    }
}